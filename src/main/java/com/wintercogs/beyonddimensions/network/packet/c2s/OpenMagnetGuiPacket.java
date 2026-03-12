package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenMagnetGuiPacket()
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player == null)
        {
            return;
        }

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null)
        {
            return;
        }

        ItemStack itemStack = InventoryHelper.findItemInPlayerInventory(player, BDItems.NET_MAGNET_ITEM.get());
        if (itemStack == null || itemStack.isEmpty())
        {
            return;
        }

        player.openMenu(new SimpleMenuProvider((containerId, inv, serverPlayer) ->
                new NetMagnetMenu(containerId, inv, itemStack),
                Component.translatable("menu.title.beyonddimensions.magnet_menu")));
    }

    public static void handle(OpenMagnetGuiPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(OpenMagnetGuiPacket packet, FriendlyByteBuf buf)
    {
    }

    public static OpenMagnetGuiPacket decode(FriendlyByteBuf buf)
    {
        return new OpenMagnetGuiPacket();
    }
}
