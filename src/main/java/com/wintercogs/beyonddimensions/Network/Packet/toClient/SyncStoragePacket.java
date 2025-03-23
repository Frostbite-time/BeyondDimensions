package com.wintercogs.beyonddimensions.Network.Packet.toClient;

import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncStoragePacket(List<IStackType> stacks, List<Long> changedCounts, List<Integer> targetIndex)
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        if (player.containerMenu instanceof DimensionsNetMenu menu)
        {
            IStackTypedHandler clientStorage = menu.storage;
            int i = 0;
            for(IStackType remoteStack : stacks())
            {
                // 如果当前存储存在此物品
                if(clientStorage.hasStackType(remoteStack))
                {
                    if(changedCounts().get(i) > 0)
                    {
                        clientStorage.insert(remoteStack.copyWithCount(changedCounts().get(i)),false);
                    }
                    else
                    {
                        clientStorage.extract(remoteStack.copyWithCount(-changedCounts().get(i)),false);
                    }
                }
                else // 如果当前存储不存在此物品
                {
                    if(changedCounts().get(i) > 0)
                    {
                        clientStorage.insert(remoteStack.copyWithCount(changedCounts().get(i)),false);
                    }
                }
                i++; // 一次遍历完毕后索引自增
            }
            menu.updateViewerStorage();
        }
        if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            IStackTypedHandler clientStorage = menu.storage;
            int i = 0;
            for(IStackType remoteStack : stacks())
            {
                if(changedCounts().get(i) > 0)
                {
                    clientStorage.insert(targetIndex().get(i),remoteStack.copyWithCount(changedCounts().get(i)),false);
                }
                else
                {
                    clientStorage.extract(targetIndex().get(i),-changedCounts().get(i),false);
                }
                i++; // 一次遍历完毕后索引自增
            }

            menu.updateViewerStorage();
        }
    }


    public static void handle(SyncStoragePacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(context))
            );
            context.setPacketHandled(true);
        }
    }

    public static void encode(SyncStoragePacket packet, FriendlyByteBuf buf)
    {
        // 序列化stacks列表
        buf.writeInt(packet.stacks().size());
        for (IStackType stack : packet.stacks()) {
            stack.serialize(buf);
        }

        // 序列化changedCounts列表（长整型）
        buf.writeInt(packet.changedCounts().size());
        for (long count : packet.changedCounts()) {
            buf.writeLong(count);
        }

        // 序列化targetIndex列表
        buf.writeInt(packet.targetIndex().size());
        for (int index : packet.targetIndex()) {
            buf.writeInt(index);
        }
    }

    public static SyncStoragePacket decode(FriendlyByteBuf buf)
    {
        // 反序列化stacks列表
        int stacksSize = buf.readInt();
        List<IStackType> stacks = new ArrayList<>(stacksSize);
        for (int i = 0; i < stacksSize; i++) {
            stacks.add(IStackType.deserializeCommon(buf));
        }

        // 反序列化changedCounts列表
        int countsSize = buf.readInt();
        List<Long> changedCounts = new ArrayList<>(countsSize);
        for (int i = 0; i < countsSize; i++) {
            changedCounts.add(buf.readLong());
        }

        // 反序列化targetIndex列表
        int indicesSize = buf.readInt();
        List<Integer> targetIndex = new ArrayList<>(indicesSize);
        for (int i = 0; i < indicesSize; i++) {
            targetIndex.add(buf.readInt());
        }

        return new SyncStoragePacket(stacks, changedCounts, targetIndex);
    }
}
