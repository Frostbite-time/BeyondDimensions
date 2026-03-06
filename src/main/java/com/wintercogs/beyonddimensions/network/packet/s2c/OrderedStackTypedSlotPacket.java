package com.wintercogs.beyonddimensions.network.packet.s2c;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 用于在任何种类的有序Slot中用于网络传输
// slotId表示其在Menu的Slots列表中的索引，用于确定Slot本身
// slotIndex表示其存储的资源在其所处的容器中的索引号
// stack表示用于覆盖的堆叠。不过这只是建议，如果你想省一些网络传输，你也可以传入空堆叠，然后自己在对应Slot类型的loadStorage中处理情况
// newAmount表示变化后的数量，不过一般stack中可以存储数量，如果你因为某些情况传入空堆叠，或者不放心stack的数量，又或者需要额外的数据标记，都可以用这个
// 只需要在自己的loadStorage中完成处理
public record OrderedStackTypedSlotPacket(int slotId, int slotIndex, IStackKey<?> stack,
                                          long newAmount) implements CustomPacketPayload
{
    public static final Type<OrderedStackTypedSlotPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("ordered_stack_typed_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrderedStackTypedSlotPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    OrderedStackTypedSlotPacket::slotId,
                    ByteBufCodecs.VAR_INT,
                    OrderedStackTypedSlotPacket::slotIndex,
                    IStackKey.STREAM_CODEC,
                    OrderedStackTypedSlotPacket::stack,
                    ByteBufCodecs.VAR_LONG,
                    OrderedStackTypedSlotPacket::newAmount,
                    OrderedStackTypedSlotPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {
        Player player = context.player();
        if (player.containerMenu instanceof AbstractContainerMenu menu)
        {
            if (menu.slots.get(this.slotId()) instanceof AbstractStackTypedSlot slot)
            {
                slot.loadChange(this.slotIndex(), this.stack(), this.newAmount());
            }
        }
    }

    private void handleInServer(final IPayloadContext context)
    {

    }

    public static void handle(final OrderedStackTypedSlotPacket packet, final IPayloadContext context)
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
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
