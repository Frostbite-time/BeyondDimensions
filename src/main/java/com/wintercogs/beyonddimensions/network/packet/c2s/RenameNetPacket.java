package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record RenameNetPacket(int netId, String customName) implements CustomPacketPayload
{
    public static final Type<RenameNetPacket> TYPE = new Type<>(BeyondDimensions.makeId("rename_net_packet"));

    public static final StreamCodec<ByteBuf, RenameNetPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RenameNetPacket::netId,
            ByteBufCodecs.STRING_UTF8,
            RenameNetPacket::customName,
            RenameNetPacket::new
    );

    private void handleInClient(final IPayloadContext context)
    {
    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();
        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null || !net.isManager(player))
            return;

        net.setCustomName(customName);
    }

    public static void handle(final RenameNetPacket packet, final IPayloadContext context)
    {
        if (packet == null)
            return;

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
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
