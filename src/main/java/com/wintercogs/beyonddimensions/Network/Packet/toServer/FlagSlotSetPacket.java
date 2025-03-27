package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;



public class FlagSlotSetPacket implements IMessage
{
    private int index;
    private IStackType clickStack;
    private IStackType flagStack;

    public FlagSlotSetPacket()
    {
    }

    public FlagSlotSetPacket(int index, IStackType clickStack, IStackType flagStack)
    {
        this.index = index;
        this.clickStack = clickStack;
        this.flagStack = flagStack;
    }

    public int getIndex()
    {
        return index;
    }

    public IStackType getClickStack()
    {
        return clickStack;
    }

    public IStackType getFlagStack()
    {
        return flagStack;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.index = buffer.readInt();
        this.clickStack = IStackType.deserializeCommon(buffer);
        this.flagStack = IStackType.deserializeCommon(buffer);

    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeInt(this.index);
        this.clickStack.serialize(buffer);
        this.flagStack.serialize(buffer);

    }


    public static class FlagSlotSetPacketHandler implements IMessageHandler<FlagSlotSetPacket, IMessage>
    {

        @Override
        public IMessage onMessage(FlagSlotSetPacket message, MessageContext ctx)
        {
            // 这是发送到服务器的数据包发送到的玩家
            EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            // 添加为一个计划任务(Scheduled Task)，在主服务器线程上执行操作
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                if (serverPlayer.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    menu.setFlagSlot(message.getIndex(), message.getClickStack(), message.getFlagStack());
                    menu.broadcastChanges();
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
            });
            // 没有回应数据包
            return null;
        }
    }


}
