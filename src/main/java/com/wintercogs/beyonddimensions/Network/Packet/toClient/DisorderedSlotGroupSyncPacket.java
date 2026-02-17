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

public record DisorderedSlotGroupSyncPacket(int groupId, List<IStackKey> stacks, List<Long> newCount)
{

    @OnlyIn(Dist.CLIENT)
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            SlotGroupSync sync = menu.slotGroupSyncs.get(groupId());
            if (sync != null)
            {
                sync.loadChange(stacks(), newCount());
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
        buf.writeVarInt(packet.groupId);

        // 序列化stacks列表
        buf.writeInt(packet.stacks().size());
        for (IStackKey stack : packet.stacks())
        {
            stack.serialize(buf);
        }

        // 序列化changedCounts列表（长整型）
        buf.writeInt(packet.newCount().size());
        for (long count : packet.newCount())
        {
            buf.writeLong(count);
        }

    }

    public static DisorderedSlotGroupSyncPacket decode(FriendlyByteBuf buf)
    {
        int groupId = buf.readVarInt();

        // 反序列化stacks列表
        int stacksSize = buf.readInt();
        List<IStackKey> stacks = new ArrayList<>(stacksSize);
        for (int i = 0; i < stacksSize; i++)
        {
            stacks.add(IStackKey.deserializeCommon(buf));
        }

        // 反序列化changedCounts列表
        int countsSize = buf.readInt();
        List<Long> changedCounts = new ArrayList<>(countsSize);
        for (int i = 0; i < countsSize; i++)
        {
            changedCounts.add(buf.readLong());
        }

        return new DisorderedSlotGroupSyncPacket(groupId, stacks, changedCounts);
    }
}
