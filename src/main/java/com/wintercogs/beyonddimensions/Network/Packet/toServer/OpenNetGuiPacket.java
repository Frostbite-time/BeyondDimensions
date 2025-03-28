package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.screen.viewport.GuiViewportStack;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.wintercogs.beyonddimensions.Gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;



import static com.wintercogs.beyonddimensions.Registry.UIRegister.DIMENSIONS_NET_GUI;

public class OpenNetGuiPacket implements IMessage
{

    private String uuid;

    public OpenNetGuiPacket()
    {}

    public OpenNetGuiPacket(String uuid)
    {
        this.uuid = uuid;
    }

    public String getUuid()
    {
        return uuid;
    }


    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.uuid = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, this.uuid);
    }

    // 这里的类型参数 A是你接受的包 B是你回应用的包，你可以回应一个null
    public static class OpenNetGuiPacketHandler implements IMessageHandler<OpenNetGuiPacket, IMessage>
    {

        @Override
        public IMessage onMessage(OpenNetGuiPacket message, MessageContext ctx)
        {
            // 这是发送到服务器的数据包发送到的玩家
            EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            // 添加为一个计划任务(Scheduled Task)，在主服务器线程上执行操作
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                //UIRegister.openGui(serverPlayer, DIMENSIONS_NET_GUI);
                SimpleGuiFactory factory =  new SimpleGuiFactory("test",() ->{
                    return new IGuiHolder<GuiData>()
                    {
                        @Override
                        public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
                        {
                            return BDBaseGUI.createPanel(guiData, guiSyncManager);
                        }
                    };
                });

                factory.open(serverPlayer);
            });
            // 没有回应数据包
            return null;

        }
    }
}
