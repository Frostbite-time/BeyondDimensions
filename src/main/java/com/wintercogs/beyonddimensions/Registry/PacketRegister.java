package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.FlagSlotSetPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.NetControlActionPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.OpenNetGuiPacket;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID)
public class PacketRegister
{

    // 定义网络通道
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(BeyondDimensions.MODID);
    private static int packetId = 1;

    static {
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

        INSTANCE.registerMessage(
                FlagSlotSetPacket.FlagSlotSetPacketHandler.class,
                FlagSlotSetPacket.class,
                packetId++,
                Side.SERVER
        );


        // to client


        INSTANCE.registerMessage(
                packetId++,
                CallSeverClickPacket.class,
                CallSeverClickPacket::encode,
                CallSeverClickPacket::decode,
                CallSeverClickPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                EnergyStoragePacket.class,
                EnergyStoragePacket::encode,
                EnergyStoragePacket::decode,
                EnergyStoragePacket::handle
        );





        INSTANCE.registerMessage(
                packetId++,
                PlayerPermissionInfoPacket.class,
                PlayerPermissionInfoPacket::encode,
                PlayerPermissionInfoPacket::decode,
                PlayerPermissionInfoPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                PopModeButtonPacket.class,
                PopModeButtonPacket::encode,
                PopModeButtonPacket::decode,
                PopModeButtonPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                SyncFlagPacket.class,
                SyncFlagPacket::encode,
                SyncFlagPacket::decode,
                SyncFlagPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                SyncStoragePacket.class,
                SyncStoragePacket::encode,
                SyncStoragePacket::decode,
                SyncStoragePacket::handle
        );
    }
}
