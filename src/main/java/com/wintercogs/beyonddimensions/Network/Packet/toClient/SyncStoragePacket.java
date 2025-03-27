package com.wintercogs.beyonddimensions.Network.Packet.toClient;

import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncStoragePacket implements IMessage
{
    private List<IStackType> stacks;
    private List<Long> changedCounts;
    private List<Integer> targetIndex;

    public SyncStoragePacket(){}

    public SyncStoragePacket(List<IStackType> stacks, List<Long> changedCounts, List<Integer> targetIndex)
    {
        this.stacks = stacks;
        this.changedCounts = changedCounts;
        this.targetIndex = targetIndex;
    }

    public List<IStackType> getStacks()
    {
        return stacks;
    }

    public List<Long> getChangedCounts()
    {
        return changedCounts;
    }

    public List<Integer> getTargetIndex()
    {
        return targetIndex;
    }


    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        // 序列化stacks列表
        buffer.writeInt(stacks.size());
        for (IStackType stack : stacks) {
            stack.serialize(buffer);
        }
        // 序列化changedCounts列表
        buffer.writeInt(changedCounts.size());
        for (long count : changedCounts) {
            buffer.writeLong(count);
        }
        // 序列化targetIndex列表
        buffer.writeInt(targetIndex.size());
        for (int index : targetIndex) {
            buffer.writeInt(index);
        }
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        // 反序列化stacks列表
        int stacksSize = buffer.readInt();
        stacks = new ArrayList<>(stacksSize);
        for (int i = 0; i < stacksSize; i++) {
            stacks.add(IStackType.deserializeCommon(buffer));
        }
        // 反序列化changedCounts列表
        int countsSize = buffer.readInt();
        changedCounts = new ArrayList<>(countsSize);
        for (int i = 0; i < countsSize; i++) {
            changedCounts.add(buffer.readLong());
        }
        // 反序列化targetIndex列表
        int indicesSize = buffer.readInt();
        targetIndex = new ArrayList<>(indicesSize);
        for (int i = 0; i < indicesSize; i++) {
            targetIndex.add(buffer.readInt());
        }
    }

    @SideOnly(Side.CLIENT)
    public static class SyncStoragePacketHandler implements IMessageHandler<SyncStoragePacket, IMessage>
    {

        @Override
        public IMessage onMessage(SyncStoragePacket message, MessageContext ctx)
        {
            // 确保在客户端主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player.openContainer instanceof DimensionsNetMenu menu)
                {
                    IStackTypedHandler clientStorage = menu.storage;
                    int i = 0;
                    for (IStackType remoteStack : message.stacks)
                    {
                        // 如果当前存储存在此物品
                        if (clientStorage.hasStackType(remoteStack))
                        {
                            if (message.changedCounts.get(i) > 0)
                            {
                                clientStorage.insert(remoteStack.copyWithCount(message.changedCounts.get(i)), false);
                            }
                            else
                            {
                                clientStorage.extract(remoteStack.copyWithCount(-message.changedCounts.get(i)), false);
                            }
                        }
                        else // 如果当前存储不存在此物品
                        {
                            if (message.changedCounts.get(i) > 0)
                            {
                                clientStorage.insert(remoteStack.copyWithCount(message.changedCounts.get(i)), false);
                            }
                        }
                        i++; // 一次遍历完毕后索引自增
                    }
                    menu.updateViewerStorage();
                }
                if (player.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    IStackTypedHandler clientStorage = menu.storage;
                    int i = 0;
                    for (IStackType remoteStack : message.stacks)
                    {
                        if (message.changedCounts.get(i) > 0)
                        {
                            clientStorage.insert(message.targetIndex.get(i), remoteStack.copyWithCount(message.changedCounts.get(i)), false);
                        }
                        else
                        {
                            clientStorage.extract(message.targetIndex.get(i), -message.changedCounts.get(i), false);
                        }
                        i++; // 一次遍历完毕后索引自增
                    }

                    menu.updateViewerStorage();
                }
            });
            return null; // 不需要回复消息
        }
    }


}
