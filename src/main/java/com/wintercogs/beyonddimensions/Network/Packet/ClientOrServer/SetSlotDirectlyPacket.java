package com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
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

public record SetSlotDirectlyPacket(int slotId, IStackType stack)
{
    private void handleServer(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        AbstractContainerMenu menu = player.containerMenu;
        if(menu != null)
        {
            if(menu.slots.get(slotId()) instanceof AbstractStackTypedSlot slot)
            {
                slot.setStackDirectly(stack());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        AbstractContainerMenu menu = player.containerMenu;
        if(menu != null)
        {
            if(menu.slots.get(slotId()) instanceof AbstractStackTypedSlot slot)
            {
                slot.setStackDirectly(stack());
            }
        }
    }


    public static void handle(SetSlotDirectlyPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            NetworkDirection direction = context.getDirection();
            if(direction == NetworkDirection.PLAY_TO_CLIENT)
            {
                context.enqueueWork(() ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handleClient(context))
                );
                context.setPacketHandled(true);
            }
            else if(direction == NetworkDirection.PLAY_TO_SERVER)
            {
                context.enqueueWork(() -> packet.handleServer(context));
                context.setPacketHandled(true);
            }
        }
    }

    public static void encode(SetSlotDirectlyPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.slotId);
        packet.stack.serialize(buf);
    }

    public static SetSlotDirectlyPacket decode(FriendlyByteBuf buf)
    {
        int slotId = buf.readVarInt();
        IStackType stack = IStackType.deserializeCommon(buf);

        return new SetSlotDirectlyPacket(slotId, stack);
    }
}
