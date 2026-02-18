package com.wintercogs.beyonddimensions.Network.Packet.toClient;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.SlotGroupSync;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record DisorderedSlotGroupSyncPacket(int groupId, List<IStackKey<?>> keys, List<Long> newCounts,
                                            List<Long> newModifiedTime,
                                            List<Long> newInsertedTime)
{

    @OnlyIn(Dist.CLIENT)
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            SlotGroupSync sync = menu.slotGroupSyncs.get(groupId());
            if (sync != null)
            {
                sync.loadChange(keys(), newCounts(), newModifiedTime(), newInsertedTime());
                sync.afterLoadChange();

            }
        }
    }


    public static void handle(DisorderedSlotGroupSyncPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(context))
            );
            context.setPacketHandled(true);
        }
    }

    public static void encode(DisorderedSlotGroupSyncPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.groupId());

        // keys
        buf.writeVarInt(packet.keys().size());
        for (IStackKey<?> key : packet.keys())
        {
            IStackKey.serializeCommon(buf, key);
        }

        // newCounts
        buf.writeVarInt(packet.newCounts().size());
        for (long v : packet.newCounts())
        {
            buf.writeLong(v);
        }

        // newModifiedTime
        buf.writeVarInt(packet.newModifiedTime().size());
        for (long v : packet.newModifiedTime())
        {
            buf.writeLong(v);
        }

        // newInsertedTime
        buf.writeVarInt(packet.newInsertedTime().size());
        for (long v : packet.newInsertedTime())
        {
            buf.writeLong(v);
        }
    }

    public static DisorderedSlotGroupSyncPacket decode(FriendlyByteBuf buf)
    {
        int groupId = buf.readVarInt();

        // keys
        int keysSize = buf.readVarInt();
        List<IStackKey<?>> keys = new ArrayList<>(keysSize);
        for (int i = 0; i < keysSize; i++)
        {
            keys.add(IStackKey.deserializeCommon(buf));
        }

        // newCounts
        int countsSize = buf.readVarInt();
        List<Long> newCounts = new ArrayList<>(countsSize);
        for (int i = 0; i < countsSize; i++)
        {
            newCounts.add(buf.readLong());
        }

        // newModifiedTime
        int modifiedSize = buf.readVarInt();
        List<Long> newModifiedTime = new ArrayList<>(modifiedSize);
        for (int i = 0; i < modifiedSize; i++)
        {
            newModifiedTime.add(buf.readLong());
        }

        // newInsertedTime
        int insertedSize = buf.readVarInt();
        List<Long> newInsertedTime = new ArrayList<>(insertedSize);
        for (int i = 0; i < insertedSize; i++)
        {
            newInsertedTime.add(buf.readLong());
        }

        return new DisorderedSlotGroupSyncPacket(groupId, keys, newCounts, newModifiedTime, newInsertedTime);
    }
}
