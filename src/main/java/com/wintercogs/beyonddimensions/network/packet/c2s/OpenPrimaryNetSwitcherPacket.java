package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenPrimaryNetSwitcherPacket()
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player == null)
        {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignoredPlayer) -> new PrimaryNetSwitcherMenu(containerId, playerInventory),
                Component.translatable("menu.title.beyonddimensions.primary_net_switcher")
        ));
    }

    public static void handle(OpenPrimaryNetSwitcherPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(OpenPrimaryNetSwitcherPacket packet, FriendlyByteBuf buf)
    {
    }

    public static OpenPrimaryNetSwitcherPacket decode(FriendlyByteBuf buf)
    {
        return new OpenPrimaryNetSwitcherPacket();
    }
}
