package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RenameNetPacket(int netId, String customName)
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player == null)
        {
            return;
        }

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null || !net.isManager(player))
        {
            return;
        }

        net.setCustomName(customName);
    }

    public static void handle(RenameNetPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(RenameNetPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.netId());
        buf.writeUtf(packet.customName());
    }

    public static RenameNetPacket decode(FriendlyByteBuf buf)
    {
        return new RenameNetPacket(buf.readVarInt(), buf.readUtf());
    }
}
