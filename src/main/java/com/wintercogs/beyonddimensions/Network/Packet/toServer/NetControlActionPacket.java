package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;


public class NetControlActionPacket implements IMessage
{
    private UUID receiver;
    private NetControlAction action;

    public NetControlActionPacket()
    {
    }

    public NetControlActionPacket(UUID receiver, NetControlAction action)
    {
        this.receiver = receiver;
        this.action = action;
    }

    public UUID getReceiver()
    {
        return receiver;
    }

    public NetControlAction getAction()
    {
        return action;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        receiver = packetBuffer.readUniqueId();
        action = packetBuffer.readEnumValue(NetControlAction.class);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeUniqueId(receiver);
        packetBuffer.writeEnumValue(action);
    }

    public static class NetControlActionPacketHandler implements IMessageHandler<NetControlActionPacket, IMessage>
    {

        @Override
        public IMessage onMessage(NetControlActionPacket message, MessageContext ctx)
        {
            // 这是发送到服务器的数据包发送到的玩家
            EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            // 添加为一个计划任务(Scheduled Task)，在主服务器线程上执行操作
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                NetControlMenu menu;
                if (!(serverPlayer.openContainer instanceof NetControlMenu))
                {
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
                menu = (NetControlMenu) serverPlayer.openContainer;
                menu.handlePlayerAction(message.getReceiver(), message.getAction());
            });
            // 没有回应数据包
            return null;
        }
    }
}
