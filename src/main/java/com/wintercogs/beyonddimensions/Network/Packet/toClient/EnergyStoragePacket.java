package com.wintercogs.beyonddimensions.Network.Packet.toClient;


import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class EnergyStoragePacket implements IMessage
{

    private long energyStored;
    private long energyCap;

    public EnergyStoragePacket(){}

    public EnergyStoragePacket(long energyStored ,long energyCap)
    {
        this.energyStored = energyStored;
        this.energyCap = energyCap;
    }

    public long getEnergyStored()
    {
        return energyStored;
    }

    public long getEnergyCap()
    {
        return energyCap;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.energyStored = buf.readLong();
        this.energyCap = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeLong(this.energyStored);
        buf.writeLong(this.energyCap);
    }

    @SideOnly(value = Side.CLIENT)
    public static class EnergyStoragePacketHandler implements IMessageHandler<EnergyStoragePacket, IMessage>
    {

        @Override
        public IMessage onMessage(EnergyStoragePacket message, MessageContext ctx)
        {
            // 确保在客户端主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;

                if (player.openContainer instanceof NetEnergyMenu) {
                    NetEnergyMenu menu = (NetEnergyMenu) player.openContainer;

                    // 使用数据包中的值更新 GUI
                    menu.loadStorage(message.getEnergyCap(), message.getEnergyStored());
                }
            });
            return null; // 不需要回复消息
        }
    }

}
