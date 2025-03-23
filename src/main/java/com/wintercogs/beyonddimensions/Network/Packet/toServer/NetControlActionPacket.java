package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record NetControlActionPacket(UUID receiver, NetControlAction action)
{

    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        NetControlMenu menu;
        if (!(player.containerMenu instanceof NetControlMenu))
        {
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
        menu = (NetControlMenu) player.containerMenu;
        menu.handlePlayerAction(receiver(),action());
    }


    public static void handle(NetControlActionPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(NetControlActionPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUUID(packet.receiver());
        buf.writeEnum(packet.action());
    }

    public static NetControlActionPacket decode(FriendlyByteBuf buf)
    {
        UUID uuid = buf.readUUID();
        NetControlAction action = buf.readEnum(NetControlAction.class);
        return new NetControlActionPacket(uuid, action);
    }


}
