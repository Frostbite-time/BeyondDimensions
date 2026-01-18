package com.wintercogs.beyonddimensions.Network.Packet.toServer;

import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClickTransferCraftButtonPacket(boolean toStorage)
{

    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        if (player.containerMenu instanceof DimensionsCraftMenu menu)
        {
            //服务端处理示意
            //1.解析数组
            //2.为每一个槽位在背包和存储中寻找资源填入
            menu.cleanCraftSlots(toStorage());

        }

    }


    public static void handle(ClickTransferCraftButtonPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(ClickTransferCraftButtonPacket packet, FriendlyByteBuf buf)
    {
        buf.writeBoolean(packet.toStorage);
    }

    public static ClickTransferCraftButtonPacket decode(FriendlyByteBuf buf)
    {
        return new ClickTransferCraftButtonPacket(buf.readBoolean());
    }
}
