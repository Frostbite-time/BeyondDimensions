package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetSwitchHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record PrimaryNetSwitchActionPacket(PrimaryNetSwitchAction action, int targetNetId)
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player == null)
        {
            return;
        }

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

    public static void handle(PrimaryNetSwitchActionPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(PrimaryNetSwitchActionPacket packet, FriendlyByteBuf buf)
    {
        buf.writeEnum(packet.action());
        buf.writeVarInt(packet.targetNetId());
    }

    public static PrimaryNetSwitchActionPacket decode(FriendlyByteBuf buf)
    {
        return new PrimaryNetSwitchActionPacket(buf.readEnum(PrimaryNetSwitchAction.class), buf.readVarInt());
    }
}
