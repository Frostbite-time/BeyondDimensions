package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.DisorderedSlotGroupSyncPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * 用于无序槽位的同步器（事件驱动 + 逐 tick 合并发送）
 * - 服务端仅入队更新，不立刻发包；
 * - updateChange() 每 tick 合并一次并分包发送；
 * - 每个 key 在一次发送周期内只发送一次，且为“最近一次”的绝对状态。
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

    /**
     * 等待发送的“最新绝对状态”缓存（同一 key 多次更新仅保留最后一次）
     * 保证每tick最多统一发送一次，防止客户端接收到过多刷新包
     */
    private final Map<IStackKey<?>, PendingRecord> pending = new HashMap<>();

    /**
     * 标记：需要在下一次 tick 做一次全量对比（Any 触发）
     */
    private boolean dirtyFullRescan = false;

    /**
     * 缓存条目：绝对数量 + UI 用时间戳
     */
    private record PendingRecord(long count, long modified, long inserted)
    {
    }

    public DisorderedSlotGroupSync(BDBaseMenu menu, int id, AbstractUnorderedStackHandler storage)
    {
        this.menu = menu;
        this.groupId = id;
        this.storage = storage;

        // 仅在服务端订阅
        if (isServerSide())
        {
            // 订阅 Any（全量结构变更 -> 仅置脏，不立刻发送）
            this.anySub = storage.subscribeAny(menu, this::onAnyChange);
            // 订阅 Delta（单次增量变更 -> 仅入队绝对状态，不立刻发送）
            this.deltaSub = storage.subscribeDelta(menu, this::onDeltaChange);
        }
    }

    /**
     * 在菜单关闭时调用，主动解订阅，避免句柄悬挂
     */
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

    /* -------------------- 事件回调（仅服务端执行，不立刻发送） -------------------- */

    /**
     * Any 回调：标记需要一次全量对比；真正的对比与发送放到下一次 tick
     */
    private void onAnyChange()
    {
        if (!isServerSide()) return;
        dirtyFullRescan = true;
    }

    /**
     * Delta 回调：仅缓存该 key 的“当前绝对状态”，不立刻发送
     */
    private void onDeltaChange(IStackKey<?> key, long size, boolean insert)
    {
        if (!isServerSide() || key == null) return;

        long countNow = storage.getStackByKey(key).amount();
        long lastModified = getLastModifiedOrZero(key);
        long insertedTime = getCreationOrZero(key);

        // 覆盖式缓存：确保同一 key 在一个发送周期内只保留最新状态
        pending.put(key, new PendingRecord(countNow, lastModified, insertedTime));
    }

    /* -------------------- 逐 tick 合并并发送（服务端） -------------------- */

    @Override
    public void updateChange()
    {
        if (!isServerSide()) return;

        // 首次：做一次全量对比（但也放在本 tick 发送逻辑里处理）
        if (!initialized)
        {
            initialized = true;
            dirtyFullRescan = true;
        }

        // 把 full-rescan 与 pending 合并为最终 toSend，再分包发送
        drainAndSend();
    }

    /**
     * 汇总待发更新（full-rescan 或 pending）-> 分包发送 -> 推进基线
     */
    private void drainAndSend()
    {
        if (!dirtyFullRescan && pending.isEmpty()) return;

        Map<IStackKey<?>, PendingRecord> toSend = new LinkedHashMap<>();

        if (dirtyFullRescan)
        {
            // === 仅做全量对比 ===
            Map<IStackKey<?>, Long> lastMap = new HashMap<>();
            for (KeyAmount ka : this.lastStorage)
            {
                lastMap.merge(ka.key(), ka.amount(), Long::sum);
            }
            Map<IStackKey<?>, Long> nowMap = new HashMap<>();
            for (KeyAmount ka : this.storage.getStorage())
            {
                nowMap.merge(ka.key(), ka.amount(), Long::sum);
            }

            Set<IStackKey<?>> allKeys = new HashSet<>();
            allKeys.addAll(lastMap.keySet());
            allKeys.addAll(nowMap.keySet());

            for (IStackKey<?> key : allKeys)
            {
                long lastCount = lastMap.getOrDefault(key, 0L);
                long nowCount = nowMap.getOrDefault(key, 0L);
                if (nowCount != lastCount)
                {
                    long mtime = getLastModifiedOrZero(key);
                    long ctime = getCreationOrZero(key);
                    toSend.put(key, new PendingRecord(nowCount, mtime, ctime));
                }
            }

            // 本轮 full-rescan 权威，丢弃本轮 pending；下一 tick 再积累新的事件
            pending.clear();
            dirtyFullRescan = false;
        }
        else
        {
            // === 仅发送 pending ===
            toSend.putAll(pending);
            pending.clear();
        }

        if (toSend.isEmpty()) return;

        // 转列表并分包发送
        List<IStackKey<?>> keys = new ArrayList<>(toSend.size());
        List<Long> counts = new ArrayList<>(toSend.size());
        List<Long> modifiedTimes = new ArrayList<>(toSend.size());
        List<Long> insertedTimes = new ArrayList<>(toSend.size());

        for (Map.Entry<IStackKey<?>, PendingRecord> e : toSend.entrySet())
        {
            keys.add(e.getKey());
            counts.add(e.getValue().count);
            modifiedTimes.add(e.getValue().modified);
            insertedTimes.add(e.getValue().inserted);
        }

        List<DisorderedSlotGroupSyncPacket> packets =
                buildBatchedPackets(keys, counts, modifiedTimes, insertedTimes);
        for (DisorderedSlotGroupSyncPacket packet : packets)
        {
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) menu.player), packet);
        }

        // 推进基线
        refreshLast();
    }

    /**
     * 估算每条记录字节大小并按 MAX_PACKET_SIZE 分包（key + count + lastModified + inserted）
     */
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

        // 预估单条大小
        for (int i = 0; i < n; i++)
        {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            IStackKey<?> k = keys.get(i);
            if (k != null) IStackKey.serializeCommon(buf, k);
            buf.writeLong(counts.get(i));
            buf.writeLong(modifiedTimes.get(i));
            buf.writeLong(insertedTimes.get(i));
            entrySizes.add(buf.readableBytes());
        }

        // 动态分包
        List<IStackKey<?>> batchKeys = new ArrayList<>();
        List<Long> batchCounts = new ArrayList<>();
        List<Long> batchModified = new ArrayList<>();
        List<Long> batchInserted = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < n; i++)
        {
            int entrySize = entrySizes.get(i);
            if (currentSize + entrySize > MAX_PACKET_SIZE && !batchKeys.isEmpty())
            {
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
        if (!batchKeys.isEmpty())
        {
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

    private long getLastModifiedOrZero(IStackKey<?> key)
    {
        Long v = storage.getLastModifiedTimeMap().get(key);
        return v == null ? 0L : v;
    }

    private long getCreationOrZero(IStackKey<?> key)
    {
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

        for (int i = 0; i < n; i++)
        {
            IStackKey<?> key = keys.get(i);
            long count = (i < newCounts.size()) ? newCounts.get(i) : 0L;
            long mtime = (i < newModifiedTime.size()) ? newModifiedTime.get(i) : 0L;
            long ctime = (i < newInsertedTime.size()) ? newInsertedTime.get(i) : 0L;

            // 直接设置绝对数量（0 会按策略移除或保留）
            if (key != null)
            {
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
    public void afterLoadChange()
    {
    }

    // 仅服务端：推进基线（每次实际发送后调用）
    public void refreshLast()
    {
        if (!isServerSide()) return;
        this.lastStorage.clear();
        this.lastStorage.addAll(this.storage.getStorage());
    }
}
