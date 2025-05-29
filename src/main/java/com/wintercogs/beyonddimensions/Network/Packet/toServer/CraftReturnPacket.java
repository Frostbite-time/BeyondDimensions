package com.wintercogs.beyonddimensions.Network.Packet.toServer;

import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CraftReturnPacket(boolean dir)
{

    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();

        if(player.containerMenu instanceof DimensionsCraftMenu menu)
        {
            menu.firstCraftReturnDir = packet.dir();
        }
    }


    public static void handle(CraftReturnPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(CraftReturnPacket packet, FriendlyByteBuf buf)
    {
        buf.writeBoolean(packet.dir());
    }

    public static CraftReturnPacket decode(FriendlyByteBuf buf)
    {
        return new CraftReturnPacket(buf.readBoolean());
    }
}
