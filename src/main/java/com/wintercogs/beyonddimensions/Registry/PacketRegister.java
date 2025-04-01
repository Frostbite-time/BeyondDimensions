package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.NetControlActionPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.OpenNetGuiPacket;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;


public class PacketRegister
{

    // 定义网络通道
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(BeyondDimensions.MODID);
    private static int packetId = 1;

    public static void registerPackets()
    {
        // to server
        INSTANCE.registerMessage(
                OpenNetGuiPacket.OpenNetGuiPacketHandler.class,
                OpenNetGuiPacket.class,
                packetId++,
                Side.SERVER
        );

        INSTANCE.registerMessage(
                NetControlActionPacket.NetControlActionPacketHandler.class,
                NetControlActionPacket.class,
                packetId++,
                Side.SERVER
        );
    }

}
