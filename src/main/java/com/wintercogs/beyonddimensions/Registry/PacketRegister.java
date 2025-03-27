package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;


@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID)
public class PacketRegister
{

    // 定义网络通道
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(BeyondDimensions.MODID);
    private static int packetId = 1;

    static {
        INSTANCE.registerMessage(
                packetId++,
                OpenNetGuiPacket.class,
                OpenNetGuiPacket::encode,
                OpenNetGuiPacket::decode,
                OpenNetGuiPacket::handle
        );

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
                FlagSlotSetPacket.class,
                FlagSlotSetPacket::encode,
                FlagSlotSetPacket::decode,
                FlagSlotSetPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                NetControlActionPacket.class,
                NetControlActionPacket::encode,
                NetControlActionPacket::decode,
                NetControlActionPacket::handle
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
