package com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer;



import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class PopModeButtonPacket implements IMessage
{

    private boolean popMode;

    public PopModeButtonPacket(){}

    public PopModeButtonPacket(boolean popMode)
    {
        this.popMode = popMode;
    }

    public boolean isPopMode()
    {
        return popMode;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.popMode = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeBoolean(popMode);
    }

    public static class PopModeButtonPacketHandlerOnServer implements IMessageHandler<PopModeButtonPacket, IMessage>
    {
        @Override
        public IMessage onMessage(PopModeButtonPacket message, MessageContext ctx)
        {
            // 这是发送到服务器的数据包发送到的玩家
            EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            // 添加为一个计划任务(Scheduled Task)，在主服务器线程上执行操作
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                if(serverPlayer.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    menu.popMode = message.popMode;
                    menu.be.popMode = message.popMode;
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
                if(serverPlayer.openContainer instanceof NetEnergyMenu menu)
                {
                    menu.popMode = message.popMode;
                    menu.be.popMode = message.popMode;
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
            });
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static class PopModeButtonPacketHandlerOnClient implements IMessageHandler<PopModeButtonPacket, IMessage>
    {

        @Override
        public IMessage onMessage(PopModeButtonPacket message, MessageContext ctx)
        {
            // 确保在客户端主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;


                if (player.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    menu.popMode = message.popMode;
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
                if (player.openContainer instanceof NetEnergyMenu menu)
                {
                    menu.popMode = message.popMode;
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
            });
            return null;
        }
    }

}
