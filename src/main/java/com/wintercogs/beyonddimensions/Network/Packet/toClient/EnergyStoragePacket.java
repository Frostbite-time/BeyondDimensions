package com.wintercogs.beyonddimensions.Network.Packet.toClient;


import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EnergyStoragePacket(long energyStored , long energyCap)
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;

        if(player.containerMenu instanceof NetEnergyMenu menu)
        {
            menu.resumeRemoteUpdates(); // 虽然本地端这个好像没有用处
            menu.loadStorage(energyCap(), energyStored());
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
    }



    public static void handle(EnergyStoragePacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(context))
            );
            context.setPacketHandled(true);
        }
    }

    public static void encode(EnergyStoragePacket packet, FriendlyByteBuf buf)
    {
        buf.writeLong(packet.energyStored());
        buf.writeLong(packet.energyCap());
    }

    public static EnergyStoragePacket decode(FriendlyByteBuf buf)
    {
        long energyStored = buf.readLong();
        long energyCap = buf.readLong();
        return new EnergyStoragePacket(energyStored, energyCap);
    }
}
