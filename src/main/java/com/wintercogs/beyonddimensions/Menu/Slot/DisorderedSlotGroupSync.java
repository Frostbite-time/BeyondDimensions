package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
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

    private boolean initialized = false; // 用于首次发送更新

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
            // 订阅 Any（全量结构变更）：做一次全量对比并分包发送
            this.anySub = storage.subscribeAny(menu, this::onAnyChange);
            // 订阅 Delta（带上下文的单次增量变更）：按细节即时发送
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

    /** Any 回调：执行一次全量对比（沿用原逻辑） */
    private void onAnyChange()
    {
        if (!isServerSide()) return;
        sendFullDiff();
    }

    /** Delta 回调：按细节即时发送（单事件 -> 单包） */
    private void onDeltaChange(IStackKey<?> key, long size, boolean insert)
    {
        if (!isServerSide()) return;
        if (key == null || size <= 0L) return;

        long delta = insert ? size : -size;

        // 直接发送单条变更
        PacketDistributor.sendToPlayer(
                (ServerPlayer) menu.player,
                new DisorderedSlotGroupSyncPacket(
                        groupId,
                        Collections.singletonList(key),
                        Collections.singletonList(delta)
                )
        );

        // 更新基线（保持与服务端真实状态一致）
        refreshLast();
    }

    /* -------------------- 全量对比并分包发送（服务端） -------------------- */

    @Override
    public void updateChange()
    {
        // 仅负责首次通知，其他时候会走回调更新
        if(initialized) return;
        if (!isServerSide()) return;
        sendFullDiff();
        initialized = true;
    }

    /** 构建 last vs now 的差量并分包发送；最后刷新基线 */
    private void sendFullDiff()
    {
        // 开始运行原子化物品比较
        ArrayList<IStackKey<?>> changedItem = new ArrayList<>();
        ArrayList<Long> changedCount = new ArrayList<>();

        // 为两个缓存数组分别创建Map
        Map<IStackKey<?>, Long> lastMap = new HashMap<>();
        for (KeyAmount stack : this.lastStorage) {
            lastMap.put(stack.key(), lastMap.getOrDefault(stack.key(), 0L) + stack.amount());
        }

        Map<IStackKey<?>, Long> nowMap = new HashMap<>();
        for (KeyAmount stack : this.storage.getStorage()) {
            nowMap.put(stack.key(), nowMap.getOrDefault(stack.key(), 0L) + stack.amount());
        }

        // 比较两个Map的差异
        Set<IStackKey<?>> allKeys = new HashSet<>();
        allKeys.addAll(lastMap.keySet());
        allKeys.addAll(nowMap.keySet());

        for (IStackKey<?> key : allKeys) {
            long lastCount = lastMap.getOrDefault(key, 0L);
            long nowCount = nowMap.getOrDefault(key, 0L);
            long delta = nowCount - lastCount;
            if (delta != 0) {
                changedItem.add(key);
                changedCount.add(delta);
            }
        }

        // 立刻更新last列表（基线推进）
        refreshLast();

        // 将数据分包发送
        if (!changedItem.isEmpty()) {
            List<DisorderedSlotGroupSyncPacket> packets = buildBatchedPackets(changedItem, changedCount);
            for (DisorderedSlotGroupSyncPacket packet : packets) {
                PacketDistributor.sendToPlayer((ServerPlayer) menu.player, packet);
            }
        }
    }

    /** 估算每条记录字节大小并按 MAX_PACKET_SIZE 分包 */
    private List<DisorderedSlotGroupSyncPacket> buildBatchedPackets(List<IStackKey<?>> items, List<Long> deltas)
    {
        final int n = items.size();
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
            IStackKey<?> stack = items.get(i);
            if (stack != null) stack.serialize(registryBuf);
            buf.writeLong(deltas.get(i));
            entrySizes.add(buf.readableBytes());
        }

        // 动态分包
        List<IStackKey<?>> batchItems = new ArrayList<>();
        List<Long>        batchCounts = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < n; i++) {
            int entrySize = entrySizes.get(i);
            if (currentSize + entrySize > MAX_PACKET_SIZE && !batchItems.isEmpty()) {
                packets.add(new DisorderedSlotGroupSyncPacket(groupId, new ArrayList<>(batchItems), new ArrayList<>(batchCounts)));
                batchItems.clear();
                batchCounts.clear();
                currentSize = 0;
            }
            batchItems.add(items.get(i));
            batchCounts.add(deltas.get(i));
            currentSize += entrySize;
        }
        if (!batchItems.isEmpty()) {
            packets.add(new DisorderedSlotGroupSyncPacket(groupId, batchItems, batchCounts));
        }
        return packets;
    }

    /* -------------------- 客户端：接收并应用 -------------------- */

    // 仅客户端 负责读取
    @Override
    public void loadChange(List<IStackKey<?>> stacks, List<Long> changedCounts)
    {
        IStackHandler clientStorage = storage;
        for (int i = 0; i < stacks.size(); i++)
        {
            IStackKey<?> remoteStack = stacks.get(i);
            long delta = changedCounts.get(i);
            if (delta > 0) {
                clientStorage.insert(remoteStack, delta, false);
            } else if (delta < 0) {
                clientStorage.extract(remoteStack, -delta, false);
            }
        }
        // 客户端不订阅事件；不需要更新 lastStorage（lastStorage 只在服务端用于构建差量）
    }

    // 仅客户端，用于后处理，建议去实际应用场景重写
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
