package com.wintercogs.beyonddimensions.network.packet.s2c;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.SlotGroupSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record DisorderedSlotGroupSyncPacket(int groupId, List<IStackKey<?>> keys, List<Long> newCounts,
                                            List<Long> newModifiedTime,
                                            List<Long> newInsertedTime) implements CustomPacketPayload
{
    public static final Type<DisorderedSlotGroupSyncPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("disordered_slot_group_sync_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DisorderedSlotGroupSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    DisorderedSlotGroupSyncPacket::groupId,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            IStackKey.STREAM_CODEC
                    ),
                    DisorderedSlotGroupSyncPacket::keys,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    DisorderedSlotGroupSyncPacket::newCounts,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    DisorderedSlotGroupSyncPacket::newModifiedTime,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    DisorderedSlotGroupSyncPacket::newInsertedTime,
                    DisorderedSlotGroupSyncPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {
        Player player = context.player();
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            SlotGroupSync sync = menu.slotGroupSyncs.get(this.groupId());
            if (sync != null)
            {
                sync.loadChange(this.keys(), this.newCounts(), this.newModifiedTime(), this.newInsertedTime());
                sync.afterLoadChange();

            }
        }
    }

    private void handleInServer(final IPayloadContext context)
    {

    }

    public static void handle(final DisorderedSlotGroupSyncPacket packet, final IPayloadContext context)
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
