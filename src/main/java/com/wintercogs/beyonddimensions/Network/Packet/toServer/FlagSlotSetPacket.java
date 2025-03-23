package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public record FlagSlotSetPacket(int index, IStackType clickStack, IStackType flagStack)
{
    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            menu.setFlagSlot(index(),clickStack(),flagStack());
            menu.broadcastChanges();
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }

    }


    public static void handle(FlagSlotSetPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(FlagSlotSetPacket packet, FriendlyByteBuf buf)
    {
        buf.writeInt(packet.index);

        IStackType clickType = packet.clickStack();
        clickType.serialize(buf); // 直接调用接口的序列化方法

        IStackType flagType = packet.flagStack();
        flagType.serialize(buf);


    }

    public static FlagSlotSetPacket decode(FriendlyByteBuf buf)
    {
        int slotIndex = buf.readInt();
        IStackType clickType = IStackType.deserializeCommon(buf); // 直接调用接口的反序列化方法
        IStackType flagType = IStackType.deserializeCommon(buf);
        return new FlagSlotSetPacket(slotIndex, clickType, flagType);
    }

}
