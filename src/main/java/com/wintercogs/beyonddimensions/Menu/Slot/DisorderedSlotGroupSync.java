package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
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

    public final int groupId;
    private final BDBaseMenu menu;
    private final UnifiedStorage storage;

    // 基线缓存：key 不含数量语义 -> 直接可做 map 的 key；value 为绝对数量
    private final Map<IStackKey, Long> lastMap = new HashMap<>();

    private boolean initialized = false;

    private AutoCloseable anySub;
    private AutoCloseable deltaSub;

    /**
     * 本 tick 待发的“最终绝对值”缓存：同一 key 仅保留最后状态
     */
    private final Map<IStackKey, Long> pendingAbsolute = new HashMap<>();

    /**
     * Any 事件置脏：下一 tick 做一次全量
     */
    private boolean dirtyFullRescan = false;

    public DisorderedSlotGroupSync(BDBaseMenu menu, int id, UnifiedStorage storage)
    {
        this.menu = menu;
        this.groupId = id;
        this.storage = storage;

        if (isServerSide())
        {
            this.anySub = storage.subscribeAny(menu, this::onAnyChange);
            this.deltaSub = storage.subscribeDelta(menu, this::onDeltaChange);
        }
    }

    public void dispose()
    {
        try
        {
            if (anySub != null) anySub.close();
        }
        catch (Throwable ignored)
        {
        }
        try
        {
            if (deltaSub != null) deltaSub.close();
        }
        catch (Throwable ignored)
        {
        }
        anySub = null;
        deltaSub = null;
    }

    private boolean isServerSide()
    {
        return menu.player instanceof ServerPlayer;
    }

    @Override
    public int getGroupId()
    {
        return groupId;
    }

    /* -------------------- 事件回调 -------------------- */

    private void onAnyChange()
    {
        if (!isServerSide()) return;
        dirtyFullRescan = true;
    }

    /**
     * 将 delta 累加到“基线 + 本 tick 已缓存的绝对值”上，得到该 key 的最终绝对值
     */
    private void onDeltaChange(IStackKey key, long size, boolean insert)
    {
        if (!isServerSide() || key == null || size == 0) return;

        long base = pendingAbsolute.getOrDefault(key, lastMap.getOrDefault(key, 0L));
        long next = insert ? base + size : base - size;
        if (next < 0) next = 0; // 保护
        pendingAbsolute.put(key, next); // 覆盖式缓存
    }

    /* -------------------- 合并并发送 -------------------- */

    @Override
    public void updateChange()
    {
        if (!isServerSide()) return;

        if (!initialized)
        {
            initialized = true;
            dirtyFullRescan = true; // 首次强制全量
        }

        drainAndSend();
    }

    private void drainAndSend()
    {
        if (!dirtyFullRescan && pendingAbsolute.isEmpty()) return;

        List<IStackKey> keys = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        if (dirtyFullRescan)
        {
            // ===== 全量：构建 nowMap，比较 lastMap 差异，发送绝对值 =====
            Map<IStackKey, Long> nowMap = buildNowMapFromStorage();

            // 合并 key 集
            Set<IStackKey> all = new HashSet<>(lastMap.keySet());
            all.addAll(nowMap.keySet());

            for (IStackKey k : all)
            {
                long last = lastMap.getOrDefault(k, 0L);
                long now = nowMap.getOrDefault(k, 0L);
                if (now != last)
                {
                    keys.add(k.copy());
                    counts.add(now);
                }
            }

            // 全量权威：清空当 tick 的 pending；基线 = nowMap
            pendingAbsolute.clear();
            lastMap.clear();
            lastMap.putAll(nowMap);
            dirtyFullRescan = false;
        }
        else
        {
            // ===== 非全量：仅发送本 tick 的最终绝对值 =====

            for (Map.Entry<IStackKey, Long> e : pendingAbsolute.entrySet())
            {
                keys.add(e.getKey().copy());
                counts.add(e.getValue());
            }

            // 发送后增量更新基线（0 → 移除；>0 → 覆盖/新增）
            applyIncrementalToBaseline(pendingAbsolute);
            pendingAbsolute.clear();
        }

        if (keys.isEmpty()) return;

        // 分包（协议：key.serialize + long 绝对数量）
        List<DisorderedSlotGroupSyncPacket> packets = buildBatchedPackets(keys, counts);
        for (DisorderedSlotGroupSyncPacket pkt : packets)
        {
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) menu.player), pkt);
        }
    }

    private Map<IStackKey, Long> buildNowMapFromStorage()
    {
        Map<IStackKey, Long> now = new HashMap<>();
        for (IStackKey st : this.storage.getStorage())
        {
            long amt = st.getStackAmount();
            if (amt <= 0) continue;
            now.merge(st, amt, Long::sum); // IStackType 不含数量 → 合并数量
        }
        return now;
    }

    /**
     * 非全量发送后把变更直接打到基线：0→remove, >0→put
     */
    private void applyIncrementalToBaseline(Map<IStackKey, Long> applied)
    {
        for (Map.Entry<IStackKey, Long> e : applied.entrySet())
        {
            IStackKey k = e.getKey();
            long v = e.getValue();
            if (v == 0L)
            {
                lastMap.remove(k);
            }
            else
            {
                lastMap.put(k, v);
            }
        }
    }

    /**
     * 分包估算：IStackType + long(绝对数量)
     */
    private List<DisorderedSlotGroupSyncPacket> buildBatchedPackets(
            List<IStackKey> keys,
            List<Long> counts
    )
    {
        final int n = keys.size();
        List<DisorderedSlotGroupSyncPacket> packets = new ArrayList<>(Math.max(1, n / 128));
        List<Integer> entrySizes = new ArrayList<>(n);

        for (int i = 0; i < n; i++)
        {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            IStackKey k = keys.get(i);
            if (k != null) k.serialize(buf);
            buf.writeLong(counts.get(i));
            entrySizes.add(buf.readableBytes());
        }

        List<IStackKey> batchKeys = new ArrayList<>();
        List<Long> batchCounts = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < n; i++)
        {
            int entrySize = entrySizes.get(i);
            if (currentSize + entrySize > MAX_PACKET_SIZE && !batchKeys.isEmpty())
            {
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
        if (!batchKeys.isEmpty())
        {
            packets.add(new DisorderedSlotGroupSyncPacket(groupId, batchKeys, batchCounts));
        }
        return packets;
    }

    /* -------------------- 客户端：覆盖式应用 -------------------- */

    @Override
    public void loadChange(List<IStackKey> stacks, List<Long> absoluteCounts)
    {
        UnifiedStorage clientStorage = storage;
        int n = Math.min(stacks.size(), absoluteCounts.size());
        for (int i = 0; i < n; i++)
        {
            IStackKey key = stacks.get(i);
            long absolute = absoluteCounts.get(i);
            if (key == null) continue;
            clientStorage.setStackAmount(key, absolute); // 覆盖
        }
    }

    @Override
    public void afterLoadChange()
    {
    }
}
