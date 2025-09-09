package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Packet.DisorderedSlotGroupSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.*;

/**
 * 用于无序槽位的同步器（事件驱动版）
 * - 服务端订阅 storage 的 Any/Delta 事件
 *   - Any：执行全量对比并分包发送
 *   - Delta：按细节即时发送（单条变更一个包）
 * - 客户端接收包后按差量应用
 */
public class DisorderedSlotGroupSync implements SlotGroupSync
{
    private static final int MAX_PACKET_SIZE = 900 * 1024; // 921,600 bytes

    public final int groupId; // 用于读取时的标记
    private final BDBaseMenu menu;
    private final AbstractUnorderedStackHandler storage; // 对真实存储的直接引用
    private final List<KeyAmount> lastStorage = new ArrayList<>();

    private boolean initialized = false; // 首次发送控制

    // 订阅句柄，便于释放
    private AutoCloseable anySub;
    private AutoCloseable deltaSub;

    public DisorderedSlotGroupSync(BDBaseMenu menu, int id, AbstractUnorderedStackHandler storage)
    {
        this.menu = menu;
        this.groupId = id;
        this.storage = storage;

        // 仅在服务端订阅，避免客户端回环
        if (isServerSide())
        {
            // 订阅 Any（全量结构变更）
            this.anySub = storage.subscribeAny(menu, this::onAnyChange);
            // 订阅 Delta（单次增量变更）
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

    private boolean isServerSide() {
        return menu.player instanceof ServerPlayer;
    }

    @Override
    public int getGroupId() { return groupId; }

    /* -------------------- 事件回调（仅服务端执行） -------------------- */

    /** Any 回调：执行一次全量对比（对变化键发送“绝对数量+时间戳”） */
    private void onAnyChange()
    {
        if (!isServerSide()) return;
        sendFullDiff();
    }

    /** Delta 回调：按细节即时发送（单事件 -> 单包，内容为当前“绝对数量+时间戳”） */
    private void onDeltaChange(IStackKey<?> key, long size, boolean insert)
    {
        if (!isServerSide()) return;
        if (key == null) return;

        // 直接读取当前数量（绝对值）
        long countNow = storage.getStackByKey(key).amount();

        // UI 时间戳：可能不存在则发 0
        long lastModified = getLastModifiedOrZero(key);
        long insertedTime = getCreationOrZero(key);

        PacketDistributor.sendToPlayer(
                (ServerPlayer) menu.player,
                new DisorderedSlotGroupSyncPacket(
                        groupId,
                        Collections.singletonList(key),
                        Collections.singletonList(countNow),
                        Collections.singletonList(lastModified),
                        Collections.singletonList(insertedTime)
                )
        );

        // 更新基线（保持与服务端真实状态一致）
        refreshLast();
    }

    /* -------------------- 全量对比并分包发送（服务端） -------------------- */

    @Override
    public void updateChange()
    {
        // 仅首次全量推送；之后依赖事件驱动
        if(initialized) return;
        if (!isServerSide()) return;
        sendFullDiff();
        initialized = true;
    }

    /** 构建 last vs now 的差量（按绝对数量）并分包发送；最后刷新基线 */
    private void sendFullDiff()
    {
        // 统计 last（基线）与 now（当前）的计数
        Map<IStackKey<?>, Long> lastMap = new HashMap<>();
        for (KeyAmount ka : this.lastStorage) {
            lastMap.merge(ka.key(), ka.amount(), Long::sum);
        }

        Map<IStackKey<?>, Long> nowMap = new HashMap<>();
        for (KeyAmount ka : this.storage.getStorage()) {
            nowMap.merge(ka.key(), ka.amount(), Long::sum);
        }

        // 变化集合 = 并集
        Set<IStackKey<?>> allKeys = new HashSet<>();
        allKeys.addAll(lastMap.keySet());
        allKeys.addAll(nowMap.keySet());

        // 收集“需要发送”的绝对值与时间戳
        ArrayList<IStackKey<?>> changedKeys = new ArrayList<>();
        ArrayList<Long> newCounts = new ArrayList<>();
        ArrayList<Long> newModifiedTimes = new ArrayList<>();
        ArrayList<Long> newInsertedTimes = new ArrayList<>();

        for (IStackKey<?> key : allKeys) {
            long lastCount = lastMap.getOrDefault(key, 0L);
            long nowCount  = nowMap.getOrDefault(key, 0L);
            if (nowCount != lastCount) {
                changedKeys.add(key);
                newCounts.add(nowCount);
                newModifiedTimes.add(getLastModifiedOrZero(key));
                newInsertedTimes.add(getCreationOrZero(key));
            }
        }

        // 立刻更新last列表（基线推进）
        refreshLast();

        // 分包发送
        if (!changedKeys.isEmpty()) {
            List<DisorderedSlotGroupSyncPacket> packets =
                    buildBatchedPackets(changedKeys, newCounts, newModifiedTimes, newInsertedTimes);
            for (DisorderedSlotGroupSyncPacket packet : packets) {
                PacketDistributor.sendToPlayer((ServerPlayer) menu.player, packet);
            }
        }
    }

    /** 估算每条记录字节大小并按 MAX_PACKET_SIZE 分包（key + count + lastModified + inserted） */
    private List<DisorderedSlotGroupSyncPacket> buildBatchedPackets(
            List<IStackKey<?>> keys,
            List<Long> counts,
            List<Long> modifiedTimes,
            List<Long> insertedTimes
    )
    {
        final int n = keys.size();
        List<DisorderedSlotGroupSyncPacket> packets = new ArrayList<>(Math.max(1, n / 128));
        List<Integer> entrySizes = new ArrayList<>(n);

        // 预估单条大小（真实序列化到临时buf测字节数）
        for (int i = 0; i < n; i++) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(
                    buf,
                    menu.player.level().registryAccess(),
                    ConnectionType.OTHER
            );
            IStackKey<?> k = keys.get(i);
            if (k != null) k.serialize(registryBuf);
            buf.writeLong(counts.get(i));
            buf.writeLong(modifiedTimes.get(i));
            buf.writeLong(insertedTimes.get(i));
            entrySizes.add(buf.readableBytes());
        }

        // 动态分包
        List<IStackKey<?>> batchKeys = new ArrayList<>();
        List<Long> batchCounts       = new ArrayList<>();
        List<Long> batchModified     = new ArrayList<>();
        List<Long> batchInserted     = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < n; i++) {
            int entrySize = entrySizes.get(i);
            if (currentSize + entrySize > MAX_PACKET_SIZE && !batchKeys.isEmpty()) {
                packets.add(new DisorderedSlotGroupSyncPacket(
                        groupId,
                        new ArrayList<>(batchKeys),
                        new ArrayList<>(batchCounts),
                        new ArrayList<>(batchModified),
                        new ArrayList<>(batchInserted)
                ));
                batchKeys.clear();
                batchCounts.clear();
                batchModified.clear();
                batchInserted.clear();
                currentSize = 0;
            }
            batchKeys.add(keys.get(i));
            batchCounts.add(counts.get(i));
            batchModified.add(modifiedTimes.get(i));
            batchInserted.add(insertedTimes.get(i));
            currentSize += entrySize;
        }
        if (!batchKeys.isEmpty()) {
            packets.add(new DisorderedSlotGroupSyncPacket(
                    groupId,
                    batchKeys,
                    batchCounts,
                    batchModified,
                    batchInserted
            ));
        }
        return packets;
    }

    private long getLastModifiedOrZero(IStackKey<?> key) {
        Long v = storage.getLastModifiedTimeMap().get(key);
        return v == null ? 0L : v;
    }

    private long getCreationOrZero(IStackKey<?> key) {
        Long v = storage.getCreationTimeMap().get(key);
        return v == null ? 0L : v;
    }

    /* -------------------- 客户端：接收并应用 -------------------- */

    // 仅客户端 负责读取（新协议：绝对数量 + 时间戳）
    @Override
    public void loadChange(List<IStackKey<?>> keys,
                           List<Long> newCounts,
                           List<Long> newModifiedTime,
                           List<Long> newInsertedTime)
    {
        AbstractUnorderedStackHandler clientStorage = storage; // 同一实现，但客户端侧不订阅事件回环
        final int n = keys.size();

        // 容错：保证四个列表等长
        if (newCounts.size() != n || newModifiedTime.size() != n || newInsertedTime.size() != n) {
            // 简单保护：只按 keys 的长度处理
        }

        for (int i = 0; i < n; i++)
        {
            IStackKey<?> key = keys.get(i);
            long count       = (i < newCounts.size()) ? newCounts.get(i) : 0L;
            long mtime       = (i < newModifiedTime.size()) ? newModifiedTime.get(i) : 0L;
            long ctime       = (i < newInsertedTime.size()) ? newInsertedTime.get(i) : 0L;

            // 直接设置绝对数量（0 会按策略移除或保留）
            if (key != null) {
                clientStorage.setAmountByKey(key, count);

                // 写 UI 时间戳（这两个 Map 在抽象类中始终存在）
                storage.setLastModifiedTime(key, mtime);
                storage.setCreationTime(key, ctime);
            }
        }
        // 客户端不维护 lastStorage；由服务端基线负责差量构建
    }

    // 仅客户端，用于后处理，建议去实际应用场景重写（比如刷新屏幕、聚焦位置等）
    @Override
    public void afterLoadChange() { }

    // 仅服务端：推进基线
    public void refreshLast()
    {
        if (!isServerSide()) return;
        this.lastStorage.clear();
        this.lastStorage.addAll(this.storage.getStorage());
    }
}
