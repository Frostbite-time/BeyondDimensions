package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClickTransferCraftButtonPacket(boolean toStorage) implements CustomPacketPayload
{
    public static final Type<ClickTransferCraftButtonPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("click_transfer_craft_button_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClickTransferCraftButtonPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ClickTransferCraftButtonPacket::toStorage,
                    ClickTransferCraftButtonPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();

        if (player.containerMenu instanceof DimensionsCraftMenu menu)
        {
            menu.cleanCraftSlots(this.toStorage());
        }
    }

    public static void handle(final ClickTransferCraftButtonPacket packet, final IPayloadContext context)
    {
        if (packet != null)
        {
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
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
