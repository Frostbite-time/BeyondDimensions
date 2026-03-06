package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CallSeverClickPacket(int slotIndex, KeyAmount clickItem, int button, boolean shiftDown)
{
    private void handleServer(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player != null && player.containerMenu instanceof BDBaseMenu menu)
        {
            menu.customClickHandler(slotIndex(), clickItem(), button(), shiftDown());
            menu.broadcastChanges();
        }
    }

    public static void handle(CallSeverClickPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handleServer(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(CallSeverClickPacket packet, FriendlyByteBuf buf)
    {
        buf.writeInt(packet.slotIndex());
        KeyAmount.serialize(buf, packet.clickItem());
        buf.writeInt(packet.button());
        buf.writeBoolean(packet.shiftDown());
    }

    public static CallSeverClickPacket decode(FriendlyByteBuf buf)
    {
        int slotIndex = buf.readInt();
        KeyAmount clickItem = KeyAmount.deserialize(buf);
        int button = buf.readInt();
        boolean shiftDown = buf.readBoolean();
        return new CallSeverClickPacket(slotIndex, clickItem, button, shiftDown);
    }
}
