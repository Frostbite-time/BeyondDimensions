package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.menu.PrimaryNetSwitcherMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenPrimaryNetSwitcherPacket() implements CustomPacketPayload
{
    public static final Type<OpenPrimaryNetSwitcherPacket> TYPE = new Type<>(BeyondDimensions.makeId("open_primary_net_switcher_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPrimaryNetSwitcherPacket> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(RegistryFriendlyByteBuf byteBuf, OpenPrimaryNetSwitcherPacket packet)
        {
        }

        @Override
        public OpenPrimaryNetSwitcherPacket decode(RegistryFriendlyByteBuf byteBuf)
        {
            return new OpenPrimaryNetSwitcherPacket();
        }
    };

    private void handleInClient(final IPayloadContext context)
    {
    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignoredPlayer) -> new PrimaryNetSwitcherMenu(containerId, playerInventory),
                Component.translatable("menu.title.beyonddimensions.primary_net_switcher")
        ));
    }

    public static void handle(final OpenPrimaryNetSwitcherPacket packet, final IPayloadContext context)
    {
        if (packet == null)
        {
            return;
        }

        PacketFlow direction = context.flow();
        if (direction == PacketFlow.CLIENTBOUND)
        {
            context.enqueueWork(() -> packet.handleInClient(context));
        }
        else if (direction == PacketFlow.SERVERBOUND)
        {
            context.enqueueWork(() -> packet.handleInServer(context));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
