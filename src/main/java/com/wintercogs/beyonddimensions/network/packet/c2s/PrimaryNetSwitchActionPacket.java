package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction action,
                                           int targetNetId) implements CustomPacketPayload
{
    public static final Type<PrimaryNetSwitchActionPacket> TYPE = new Type<>(BeyondDimensions.makeId("primary_net_switch_action_packet"));

    public static final StreamCodec<ByteBuf, PrimaryNetSwitchAction> ACTION_STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public @NotNull PrimaryNetSwitchAction decode(@NotNull ByteBuf buf)
        {
            return PrimaryNetSwitchAction.valueOf(Utf8String.read(buf, 32000));
        }

        @Override
        public void encode(@NotNull ByteBuf buf, PrimaryNetSwitchAction action)
        {
            Utf8String.write(buf, action.name(), 32000);
        }
    };

    public static final StreamCodec<ByteBuf, PrimaryNetSwitchActionPacket> STREAM_CODEC = StreamCodec.composite(
            ACTION_STREAM_CODEC,
            PrimaryNetSwitchActionPacket::action,
            ByteBufCodecs.VAR_INT,
            PrimaryNetSwitchActionPacket::targetNetId,
            PrimaryNetSwitchActionPacket::new
    );

    private void handleInClient(final IPayloadContext context)
    {
    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();
        switch (action)
        {
            case CYCLE_NEXT -> handleCycle(player);
            case SET_EXPLICIT -> handleSetExplicit(player, targetNetId);
            case CLEAR_PRIMARY -> handleClearPrimary(player);
        }
    }

    private static void handleCycle(Player player)
    {
        List<DimensionsNet> nets = DimensionsNet.getAllNetFromPlayer(player);
        if (nets.isEmpty())
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.none_available"));
            return;
        }

        DimensionsNet currentPrimaryNet = DimensionsNet.getPrimaryNetFromPlayer(player);
        int nextNetId = PrimaryNetSwitchHelper.findNextPrimaryNetId(
                nets.stream().map(DimensionsNet::getId).toList(),
                currentPrimaryNet == null ? DimensionsNet.NO_PRIMARY_NET_ID : currentPrimaryNet.getId()
        );

        if (nextNetId == DimensionsNet.NO_PRIMARY_NET_ID)
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.none_available"));
            return;
        }

        DimensionsNet nextNet = DimensionsNet.getNetFromId(nextNetId);
        if (nextNet == null)
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.invalid_target"));
            return;
        }

        if (DimensionsNet.setPrimaryNetForPlayer(player, nextNet))
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.changed", nextNetId));
        }
        else
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.no_other"));
        }
    }

    private static void handleSetExplicit(Player player, int targetNetId)
    {
        boolean stillMember = DimensionsNet.getAllNetFromPlayer(player).stream().anyMatch(net -> net.getId() == targetNetId);
        if (!stillMember)
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.invalid_target"));
            return;
        }

        DimensionsNet targetNet = DimensionsNet.getNetFromId(targetNetId);
        if (targetNet == null)
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.invalid_target"));
            return;
        }

        if (DimensionsNet.setPrimaryNetForPlayer(player, targetNet) || DimensionsNet.getPrimaryNetFromPlayer(player) == targetNet)
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.changed", targetNetId));
        }
        else
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.invalid_target"));
        }
    }

    private static void handleClearPrimary(Player player)
    {
        if (!DimensionsNet.hasAnyNet(player) && !DimensionsNet.hasPrimaryNet(player))
        {
            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.none_available"));
            return;
        }

        DimensionsNet.clearPrimaryNetForPlayer(player);
        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.primary_net.switch.cleared"));
    }

    public static void handle(final PrimaryNetSwitchActionPacket packet, final IPayloadContext context)
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
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
