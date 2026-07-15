package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.util.WysiwygHarvestHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record WysiwygHarvestPacket() implements CustomPacketPayload
{
    public static final Type<WysiwygHarvestPacket> TYPE = new Type<>(BeyondDimensions.makeId("wysiwyg_harvest_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WysiwygHarvestPacket> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull WysiwygHarvestPacket packet)
        {
        }

        @Override
        public @NotNull WysiwygHarvestPacket decode(@NotNull RegistryFriendlyByteBuf buf)
        {
            return new WysiwygHarvestPacket();
        }
    };

    public static void handle(final WysiwygHarvestPacket packet, final IPayloadContext context)
    {
        if (packet != null && context.flow() == PacketFlow.SERVERBOUND)
        {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player)
                {
                    WysiwygHarvestHandler.harvest(player);
                }
            });
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
