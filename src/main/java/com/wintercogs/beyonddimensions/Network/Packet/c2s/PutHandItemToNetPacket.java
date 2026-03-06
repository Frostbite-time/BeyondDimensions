package com.wintercogs.beyonddimensions.network.Packet.c2s;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PutHandItemToNetPacket(InteractionHand hand)
{

    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player.getMainHandItem().isEmpty()) return;
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) return;
        UnifiedStorage storage = net.getUnifiedStorage();
        KeyAmount remaining = storage.insert(new ItemStackKey(player.getMainHandItem()), player.getMainHandItem().getCount(), false);
        player.getMainHandItem().setCount((BDMath.clampLongToInt(remaining.amount())));
    }


    public static void handle(PutHandItemToNetPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(PutHandItemToNetPacket packet, FriendlyByteBuf buf)
    {
        buf.writeEnum(packet.hand);
    }

    public static PutHandItemToNetPacket decode(FriendlyByteBuf buf)
    {
        return new PutHandItemToNetPacket(buf.readEnum(InteractionHand.class));
    }
}
