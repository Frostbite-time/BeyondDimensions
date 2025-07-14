package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Packet.DisorderedSlotGroupSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.*;

// 用于无序槽位的同步器
// 不用管自身有哪些槽位，仅负责同步列表数据
// 服务端中负责整理同步数据和发送
// 客户端中负责处理接收接收后处理
public class DisorderedSlotGroupSync implements SlotGroupSync
{
    public final int groupId; // 用于读取时的标记
    private final BDBaseMenu menu;
    private final IStackTypedHandler storage; //对真实存储的直接引用
    private final List<IStackType> lastStorage = new ArrayList<>();

    public DisorderedSlotGroupSync(BDBaseMenu menu,int id,IStackTypedHandler storage)
    {
        this.menu = menu;
        this.groupId = id;
        this.storage = storage;
    }

    @Override
    public int getGroupId()
    {
        return groupId;
    }

    // 负责处理变化和发包 仅服务端
    @Override
    public void updateChange()
    {
        // 开始运行原子化物品比较
        ArrayList<IStackType> changedItem = new ArrayList<>();
        ArrayList<Long> changedCount = new ArrayList<>();

        // 为两个缓存数组分别创建Map
        Map<IStackType, Long> lastMap = new HashMap<>();
        for (IStackType stack : this.lastStorage) {
            lastMap.put(stack, lastMap.getOrDefault(stack, (long) 0) + stack.getStackAmount());
        }

        Map<IStackType, Long> nowMap = new HashMap<>();
        for (IStackType stack : this.storage.getStorage()) {
            nowMap.put(stack, nowMap.getOrDefault(stack, (long) 0) + stack.getStackAmount());
        }
        // 缓存结束后，立刻更新last列表
        refreshLast();

        // 比较两个Map的差异
        Set<IStackType> allKeys = new HashSet<>();
        allKeys.addAll(lastMap.keySet());
        allKeys.addAll(nowMap.keySet());

        for (IStackType key : allKeys) {
            long lastCount = lastMap.getOrDefault(key, (long) 0);
            long nowCount = nowMap.getOrDefault(key, (long) 0);
            long delta = nowCount - lastCount;

            if (delta != 0) {
                changedItem.add(key.copy()); // 获取基础物品的拷贝
                changedCount.add(delta);
            }
        }



        // 将数据分包发送
        if (!changedItem.isEmpty()) {
            final int MAX_PACKET_SIZE = 900 * 1024; // 921,600 bytes
            List<DisorderedSlotGroupSyncPacket> packets = new ArrayList<>();
            // 预计算条目大小
            List<Integer> entrySizes = new ArrayList<>();
            for (int i = 0; i < changedItem.size(); i++) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(
                        buf,
                        menu.player.level().registryAccess(),
                        ConnectionType.OTHER
                );

                // 序列化物品和数量
                IStackType stack = changedItem.get(i);
                if (stack != null) {
                    stack.serialize(registryBuf);
                }
                buf.writeLong(changedCount.get(i)); // 写入数量变化

                entrySizes.add(buf.readableBytes());
            }
            // 动态分包
            List<IStackType> batchItems = new ArrayList<>();
            List<Long> batchCounts = new ArrayList<>();
            int currentSize = 0;
            for (int i = 0; i < changedItem.size(); i++) {
                int entrySize = entrySizes.get(i);
                // 分包条件判断
                if (currentSize + entrySize > MAX_PACKET_SIZE) {
                    packets.add(new DisorderedSlotGroupSyncPacket(
                            groupId,
                            new ArrayList<>(batchItems),
                            new ArrayList<>(batchCounts)
                    ));

                    batchItems.clear();
                    batchCounts.clear();
                    currentSize = 0;
                }
                batchItems.add(changedItem.get(i));
                batchCounts.add(changedCount.get(i));
                currentSize += entrySize;
            }
            // 处理剩余数据
            if (!batchItems.isEmpty()) {
                packets.add(new DisorderedSlotGroupSyncPacket(groupId,batchItems, batchCounts));
            }
            // 发送所有分包
            for (DisorderedSlotGroupSyncPacket packet : packets) {
                PacketDistributor.sendToPlayer((ServerPlayer) menu.player, packet);
            }
        }
    }

    // 仅客户端 负责读取
    @Override
    public void loadChange(List<IStackType> stacks, List<Long> changedCounts)
    {
        IStackTypedHandler clientStorage = storage;
        int i = 0;
        for(IStackType remoteStack : stacks)
        {
            // 如果当前存储存在此物品
            if(clientStorage.hasStackType(remoteStack))
            {
                if(changedCounts.get(i) > 0)
                {
                    clientStorage.insert(remoteStack.copyWithCount(changedCounts.get(i)),false);
                }
                else
                {
                    clientStorage.extract(remoteStack.copyWithCount(-changedCounts.get(i)),false);
                }
            }
            else // 如果当前存储不存在此物品
            {
                if(changedCounts.get(i) > 0)
                {
                    clientStorage.insert(remoteStack.copyWithCount(changedCounts.get(i)),false);
                }
            }
            i++; // 一次遍历完毕后索引自增
        }


    }

    // 仅客户端，用于后处理，建议去实际应用场景重写
    @Override
    public void afterLoadChange()
    {

    }

    public void refreshLast()
    {
        this.lastStorage.clear();
        for(IStackType stack : this.storage.getStorage())
        {
            this.lastStorage.add(stack.copy());
        }
    }
}
