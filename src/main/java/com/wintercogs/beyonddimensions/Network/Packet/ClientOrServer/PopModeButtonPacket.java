package com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer;


import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PopModeButtonPacket(boolean popMode)
{
    private void handleServer(NetworkEvent.Context context)
    {
        Player player = context.getSender();

        if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            menu.popMode = popMode();
            menu.be.popMode = popMode();
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
        if(player.containerMenu instanceof NetEnergyMenu menu)
        {
            menu.popMode = popMode();
            menu.be.popMode = popMode();
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            menu.popMode = popMode();
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
        if (player.containerMenu instanceof NetEnergyMenu menu)
        {
            menu.popMode = popMode();
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
    }


    public static void handle(PopModeButtonPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            NetworkDirection direction = context.getDirection();
            if(direction == NetworkDirection.PLAY_TO_CLIENT)
            {
                context.enqueueWork(() ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handleClient(context))
                );
                context.setPacketHandled(true);
            }
            else if(direction == NetworkDirection.PLAY_TO_SERVER)
            {
                context.enqueueWork(() -> packet.handleServer(context));
                context.setPacketHandled(true);
            }
        }
    }

    public static void encode(PopModeButtonPacket packet, FriendlyByteBuf buf)
    {
        buf.writeBoolean(packet.popMode);
    }

    public static PopModeButtonPacket decode(FriendlyByteBuf buf)
    {
        boolean popMode = buf.readBoolean();
        return new PopModeButtonPacket(popMode);
    }
}
