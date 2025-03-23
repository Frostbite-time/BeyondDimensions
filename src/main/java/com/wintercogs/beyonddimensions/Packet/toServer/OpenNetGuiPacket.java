package com.wintercogs.beyonddimensions.Packet.toServer;


import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenNetGuiPacket(String uuid)
{

    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, _player) -> new DimensionsNetMenu(containerId, playerInventory, net),
                    Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
            ));
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
    }

    public static OpenNetGuiPacket decode(FriendlyByteBuf buf)
    {
        return new OpenNetGuiPacket(buf.readUtf());
    }
}
