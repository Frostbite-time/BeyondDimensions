package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record CallSeverClickPacket(int slotIndex, KeyAmount clickItem, int button,
                                   boolean shiftDown) implements CustomPacketPayload
{
    public static final Type<CallSeverClickPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("call_sever_click_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CallSeverClickPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CallSeverClickPacket::slotIndex,
                    KeyAmount.STREAM_CODEC,
                    CallSeverClickPacket::clickItem,
                    ByteBufCodecs.VAR_INT,
                    CallSeverClickPacket::button,
                    ByteBufCodecs.BOOL,
                    CallSeverClickPacket::shiftDown,
                    CallSeverClickPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            menu.customClickHandler(this.slotIndex(), this.clickItem(), this.button(), this.shiftDown());
            menu.broadcastChanges();
        }
    }

    public static void handle(final CallSeverClickPacket packet, final IPayloadContext context)
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
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
