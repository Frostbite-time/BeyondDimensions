package com.wintercogs.beyonddimensions.Network.Packet.toClient;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OrderedStackTypedSlotPacket(int slotId, int slotIndex, IStackType stack, long newAmount)
{

    @OnlyIn(Dist.CLIENT)
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        AbstractContainerMenu menu = player.containerMenu;

        if(menu != null)
        {
            if(menu.slots.get(slotId()) instanceof AbstractStackTypedSlot slot)
            {
                slot.loadChange(slotIndex(), stack(), newAmount());
            }
        }

    }



    public static void handle(OrderedStackTypedSlotPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(context))
            );
            context.setPacketHandled(true);
        }
    }

    public static void encode(OrderedStackTypedSlotPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.slotId);
        buf.writeVarInt(packet.slotIndex);
        packet.stack.serialize(buf);
        buf.writeVarLong(packet.newAmount);
    }

    public static OrderedStackTypedSlotPacket decode(FriendlyByteBuf buf)
    {
        int slotId = buf.readVarInt();
        int slotIndex = buf.readVarInt();
        IStackType stack = IStackType.deserializeCommon(buf);
        long newAmount = buf.readVarLong();

        return new OrderedStackTypedSlotPacket(slotId, slotIndex, stack, newAmount);
    }
}
