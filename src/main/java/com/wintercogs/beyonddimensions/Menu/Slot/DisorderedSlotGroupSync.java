package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.DisorderedSlotGroupSyncPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

// 用于无序槽位的同步器
// 不用管自身有哪些槽位，仅负责同步列表数据
// 服务端中负责整理同步数据和发送
// 客户端中负责处理接收接收后处理
public class DisorderedSlotGroupSync implements SlotGroupSync
{
    private static final int MAX_PACKET_SIZE = 900 * 1024; // 921,600 bytes

    public final int groupId; // 用于读取时的标记
    private final BDBaseMenu menu;
    private final UnifiedStorage storage; // 对真实存储的直接引用
    private final List<IStackType> lastStorage = new ArrayList<>();

    private boolean initialized = false; // 首次发送控制

    // 订阅句柄，便于释放
    private AutoCloseable anySub;
    private AutoCloseable deltaSub;

    /** 等待发送的“最新绝对状态”缓存（同一 key 多次更新仅保留最后一次） */
    private final Map<IStackType, Long> pendingAbsolute = new HashMap<>();

    /** 标记：需要在下一次 tick 做一次全量对比（Any 触发） */
    private boolean dirtyFullRescan = false;

    public DisorderedSlotGroupSync(BDBaseMenu menu, int id, UnifiedStorage storage)
    {
        this.menu = menu;
        this.groupId = id;
        this.storage = storage;

        // 仅在服务端订阅
        if (isServerSide())
        {
            // Any：结构/大变动，只置脏，下一 tick 再对比发送
            this.anySub = storage.subscribeAny(menu, this::onAnyChange);
            // Delta：单次增量事件 → 记录该 key 的“当前绝对数量”，不立刻发送
            this.deltaSub = storage.subscribeDelta(menu, this::onDeltaChange);
        }
    }

    /** 在菜单关闭时调用，主动解订阅，避免句柄悬挂 */
    public void dispose()
    {
        try { if (anySub != null) anySub.close(); } catch (Throwable ignored) {}
        try { if (deltaSub != null) deltaSub.close(); } catch (Throwable ignored) {}
        anySub = null;
        deltaSub = null;
    }

    private boolean isServerSide()
    {
        return menu.player instanceof ServerPlayer;
    }

    @Override
    public int getGroupId() { return groupId; }

    /* -------------------- 事件回调（仅服务端执行，不立刻发送） -------------------- */

    /** Any 回调：标记需要一次全量对比；真正的对比与发送放到下一次 tick */
    private void onAnyChange()
    {
        if (!isServerSide()) return;
        dirtyFullRescan = true;
    }

    /** Delta 回调：仅缓存“当前绝对数量”，不立刻发送（覆盖式） */
    private void onDeltaChange(IStackType key, long size, boolean insert)
    {
        if (!isServerSide() || key == null) return;
        // 读取当前绝对数量（IStackType 的 equals/hashCode 不含数量，可直接当键）
        long now = getNowAbsoluteCount(key);
        pendingAbsolute.put(key, now); // 覆盖式缓存：仅保留最新绝对状态
    }

    /* -------------------- 逐 tick 合并并发送（服务端） -------------------- */

    @Override
    public void updateChange()
    {
        if (!isServerSide()) return;

        // 首次：做一次全量对比（也放到本 tick 的发送逻辑里）
        if (!initialized) {
            initialized = true;
            dirtyFullRescan = true;
        }

        drainAndSend();
    }

    /** 汇总待发更新（full-rescan 或 pending）-> 分包发送 -> 推进基线 */
    private void drainAndSend()
    {
        if (!dirtyFullRescan && pendingAbsolute.isEmpty()) return;

        Map<IStackType, Long> toSend = new LinkedHashMap<>();

        if (dirtyFullRescan)
        {
            // === 全量对比：把 last vs now 不同的 key 的“现在值(绝对)”发出去 ===
            Map<IStackType, Long> lastMap = new HashMap<>();
            for (IStackType st : this.lastStorage) {
                lastMap.merge(st, st.getStackAmount(), Long::sum);
            }
            Map<IStackType, Long> nowMap = new HashMap<>();
            for (IStackType st : this.storage.getStorage()) {
                nowMap.merge(st, st.getStackAmount(), Long::sum);
            }

            Set<IStackType> all = new HashSet<>();
            all.addAll(lastMap.keySet());
            all.addAll(nowMap.keySet());

            for (IStackType k : all) {
                long lastCnt = lastMap.getOrDefault(k, 0L);
                long nowCnt  = nowMap.getOrDefault(k, 0L);
                if (nowCnt != lastCnt) {
                    toSend.put(k.copy(), nowCnt); // 发送现在值（覆盖式）
                }
            }

            // 全量权威：清空本轮 pending；下一 tick 重新积累
            pendingAbsolute.clear();
            dirtyFullRescan = false;
        }
        else
        {
            // === 仅发送 pending 的“最新绝对状态” ===
            for (Map.Entry<IStackType, Long> e : pendingAbsolute.entrySet()) {
                toSend.put(e.getKey().copy(), e.getValue());
            }
            pendingAbsolute.clear();
        }

        if (toSend.isEmpty()) {
            refreshLast(); // 仍推进基线
            return;
        }

        // 转列表并分包发送（协议：IStackType + long(绝对数量)）
        List<IStackType> keys  = new ArrayList<>(toSend.size());
        List<Long> counts      = new ArrayList<>(toSend.size());
        for (Map.Entry<IStackType, Long> e : toSend.entrySet()) {
            keys.add(e.getKey());
            counts.add(e.getValue());
        }

        List<DisorderedSlotGroupSyncPacket> packets = buildBatchedPackets(keys, counts);
        for (DisorderedSlotGroupSyncPacket pkt : packets) {
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) menu.player), pkt);
        }

        // 推进基线
        refreshLast();
    }

    /** 估算每条记录字节大小并按 MAX_PACKET_SIZE 分包（key + absoluteCount） */
    private List<DisorderedSlotGroupSyncPacket> buildBatchedPackets(
            List<IStackType> keys,
            List<Long> counts
    )
    {
        final int n = keys.size();
        List<DisorderedSlotGroupSyncPacket> packets = new ArrayList<>(Math.max(1, n / 128));
        List<Integer> entrySizes = new ArrayList<>(n);

        // 预估单条大小（按旧版写法：key.serialize + long）
        for (int i = 0; i < n; i++) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            IStackType k = keys.get(i);
            if (k != null) k.serialize(buf);
            buf.writeLong(counts.get(i));
            entrySizes.add(buf.readableBytes());
        }

        // 动态分包
        List<IStackType> batchKeys = new ArrayList<>();
        List<Long>       batchCounts = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < n; i++) {
            int entrySize = entrySizes.get(i);
            if (currentSize + entrySize > MAX_PACKET_SIZE && !batchKeys.isEmpty()) {
                packets.add(new DisorderedSlotGroupSyncPacket(
                        groupId,
                        new ArrayList<>(batchKeys),
                        new ArrayList<>(batchCounts)
                ));
                batchKeys.clear();
                batchCounts.clear();
                currentSize = 0;
            }
            batchKeys.add(keys.get(i));
            batchCounts.add(counts.get(i));
            currentSize += entrySize;
        }
        if (!batchKeys.isEmpty()) {
            packets.add(new DisorderedSlotGroupSyncPacket(
                    groupId,
                    batchKeys,
                    batchCounts
            ));
        }
        return packets;
    }

    /* -------------------- 客户端：接收并覆盖写入（绝对数量） -------------------- */

    @Override
    public void loadChange(List<IStackType> stacks, List<Long> absoluteCounts)
    {
        UnifiedStorage clientStorage = storage;
        int n = Math.min(stacks.size(), absoluteCounts.size());

        for (int i = 0; i < n; i++) {
            IStackType key = stacks.get(i);
            long absolute = absoluteCounts.get(i);
            if (key == null) continue;
            // 覆盖写入为“绝对数量”
            clientStorage.setStackAmount(key, absolute);
        }
    }

    @Override
    public void afterLoadChange() { }

    /** 仅服务端：推进基线（每次实际发送后调用） */
    public void refreshLast()
    {
        if (!isServerSide()) return;
        this.lastStorage.clear();
        for (IStackType st : this.storage.getStorage()) {
            this.lastStorage.add(st.copy()); // 数量留在副本中
        }
    }

    /** 读取指定 key 的当前绝对数量（IStackType 等价性不含数量，需聚合） */
    private long getNowAbsoluteCount(IStackType key)
    {
        IStackType current = this.storage.getStackByStack(key);
        if(!current.isEmpty())
            return current.getStackAmount();
        else
            return 0L;
    }
}
