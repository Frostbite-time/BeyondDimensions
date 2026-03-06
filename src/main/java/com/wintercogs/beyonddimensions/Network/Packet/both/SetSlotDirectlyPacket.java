package com.wintercogs.beyonddimensions.network.Packet.both;

import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetSlotDirectlyPacket(int slotId, KeyAmount stack)
{
    private void handleServer(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null)
        {
            if (menu.slots.get(slotId()) instanceof AbstractStackTypedSlot slot)
            {
                slot.setStackDirectly(stack().key(), stack().amount());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null)
        {
            if (menu.slots.get(slotId()) instanceof AbstractStackTypedSlot slot)
            {
                slot.setStackDirectly(stack().key(), stack().amount());
            }
        }
    }


    public static void handle(SetSlotDirectlyPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            NetworkDirection direction = context.getDirection();
            if (direction == NetworkDirection.PLAY_TO_CLIENT)
            {
                context.enqueueWork(() ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handleClient(context))
                );
                context.setPacketHandled(true);
            }
            else if (direction == NetworkDirection.PLAY_TO_SERVER)
            {
                context.enqueueWork(() -> packet.handleServer(context));
                context.setPacketHandled(true);
            }
        }
    }

    public static void encode(SetSlotDirectlyPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.slotId);
        KeyAmount.serialize(buf, packet.stack);
    }

    public static SetSlotDirectlyPacket decode(FriendlyByteBuf buf)
    {
        int slotId = buf.readVarInt();
        KeyAmount stack = KeyAmount.deserialize(buf);

        return new SetSlotDirectlyPacket(slotId, stack);
    }
}
