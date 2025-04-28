package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenNetGuiPacket(String uuid, boolean isCraft)
{

    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            if(isCraft())
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsCraftMenu(containerId, playerInventory, net),
                        Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                ));
            }
            else
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsNetMenu(UIRegister.Dimensions_Net_Menu.get(),containerId, playerInventory, net),
                        Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                ));
            }

        }
    }


    public static void handle(OpenNetGuiPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(OpenNetGuiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.uuid);
        buf.writeBoolean(packet.isCraft);
    }

    public static OpenNetGuiPacket decode(FriendlyByteBuf buf)
    {
        return new OpenNetGuiPacket(buf.readUtf(),buf.readBoolean());
    }
}
