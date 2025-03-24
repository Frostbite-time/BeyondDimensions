package com.wintercogs.beyonddimensions.Network.Packet.toClient;


import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
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

public record SyncFlagPacket(List<IStackType> flags, List<Integer> targetIndex)
{

    @OnlyIn(Dist.CLIENT)
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        if (player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            IStackTypedHandler clientStorage = menu.flagStorage;
            int i = 0;
            for (IStackType remoteStack : flags())
            {
                clientStorage.setStackDirectly(targetIndex().get(i), remoteStack.copyWithCount(1));
                i++; // 一次遍历完毕后索引自增
            }

            menu.updateViewerStorage();
        }
    }


    public static void handle(SyncFlagPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(context))
            );
            context.setPacketHandled(true);
        }
    }

    public static void encode(SyncFlagPacket packet, FriendlyByteBuf buf)
    {
        // 序列化flags列表
        buf.writeInt(packet.flags().size());  // 先写入列表长度
        for (IStackType flag : packet.flags()) {
            flag.serialize(buf);  // 逐个序列化IStackType
        }

        // 序列化targetIndex列表
        buf.writeInt(packet.targetIndex().size());
        for (int index : packet.targetIndex()) {
            buf.writeInt(index);
        }
    }

    public static SyncFlagPacket decode(FriendlyByteBuf buf)
    {
        // 反序列化flags列表
        int flagsSize = buf.readInt();
        List<IStackType> flags = new ArrayList<>(flagsSize);
        for (int i = 0; i < flagsSize; i++) {
            flags.add(IStackType.deserializeCommon(buf));
        }

        // 反序列化targetIndex列表
        int indicesSize = buf.readInt();
        List<Integer> targetIndex = new ArrayList<>(indicesSize);
        for (int i = 0; i < indicesSize; i++) {
            targetIndex.add(buf.readInt());
        }

        return new SyncFlagPacket(flags, targetIndex);
    }
}
