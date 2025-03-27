package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer.CallSeverClickPacket;
import com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer.PopModeButtonPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.EnergyStoragePacket;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.PlayerPermissionInfoPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.SyncFlagPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.SyncStoragePacket;
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
                EnergyStoragePacket.EnergyStoragePacketHandler.class,
                EnergyStoragePacket.class,
                packetId++,
                Side.CLIENT
        );

        INSTANCE.registerMessage(
                PlayerPermissionInfoPacket.PlayerPermissionInfoPacketHandler.class,
                PlayerPermissionInfoPacket.class,
                packetId++,
                Side.CLIENT
        );

        INSTANCE.registerMessage(
                SyncFlagPacket.SyncFlagPacketHandler.class,
                SyncFlagPacket.class,
                packetId++,
                Side.CLIENT
        );

        INSTANCE.registerMessage(
                SyncStoragePacket.SyncStoragePacketHandler.class,
                SyncStoragePacket.class,
                packetId++,
                Side.CLIENT
        );


        // 双端
        INSTANCE.registerMessage(
                CallSeverClickPacket.CallSeverClickPacketHandlerOnServer.class,
                CallSeverClickPacket.class,
                packetId++,
                Side.SERVER
        );

        INSTANCE.registerMessage(
                CallSeverClickPacket.CallSeverClickPacketHandlerOnClient.class,
                CallSeverClickPacket.class,
                packetId++,
                Side.CLIENT
        );


        INSTANCE.registerMessage(
                PopModeButtonPacket.PopModeButtonPacketHandlerOnServer.class,
                PopModeButtonPacket.class,
                packetId++,
                Side.SERVER
        );

        INSTANCE.registerMessage(
                PopModeButtonPacket.PopModeButtonPacketHandlerOnClient.class,
                PopModeButtonPacket.class,
                packetId++,
                Side.CLIENT
        );


    }
}
