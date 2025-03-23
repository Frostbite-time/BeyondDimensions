package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PacketRegister
{

    // 定义网络通道
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BeyondDimensions.MODID, "simple_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 1;

    static {

    }

//    @SubscribeEvent
//    public static void register(final RegisterPayloadHandlersEvent event)
//    {
//        //设置当前网络版本
//        final PayloadRegistrar registrar = event.registrar("1");
//
//        // 注册OpenNetGuiPacket 用于打开当前角色绑定的维度网络GUI
//        registrar.playBidirectional(
//                OpenNetGuiPacket.TYPE,
//                OpenNetGuiPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleOpenNetGuiPacket,
//                        ServerPayloadHandler.getInstance()::handleOpenNetGuiPacket
//                )
//
//        );
//
//        // 注册 ItemStoragePacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                StoragePacket.TYPE,
//                StoragePacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleStoragePacket,
//                        ServerPayloadHandler.getInstance()::handleItemStoragePacket
//                )
//        );
//
//        // 注册 CallSeverStoragePacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                CallSeverStoragePacket.TYPE,
//                CallSeverStoragePacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleCallSeverStoragePacket,
//                        ServerPayloadHandler.getInstance()::handleCallSeverStoragePacket
//                )
//        );
//
//        // 注册 SyncItemStoragePacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                SyncStoragePacket.TYPE,
//                SyncStoragePacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleSyncItemStoragePacket,
//                        ServerPayloadHandler.getInstance()::handleSyncItemStoragePacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                CallSeverClickPacket.TYPE,
//                CallSeverClickPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleCallSeverClickPacket,
//                        ServerPayloadHandler.getInstance()::handleCallSeverClickPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                CallServerPlayerInfoPacket.TYPE,
//                CallServerPlayerInfoPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleCallServerPlayerInfoPacket,
//                        ServerPayloadHandler.getInstance()::handleCallServerPlayerInfoPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                PlayerPermissionInfoPacket.TYPE,
//                PlayerPermissionInfoPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handlePlayerPermissionInfoPacket,
//                        ServerPayloadHandler.getInstance()::handlePlayerPermissionInfoPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                NetControlActionPacket.TYPE,
//                NetControlActionPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleNetControlActionPacket,
//                        ServerPayloadHandler.getInstance()::handleNetControlActionPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                SyncFlagPacket.TYPE,
//                SyncFlagPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleSyncFlagPacket,
//                        ServerPayloadHandler.getInstance()::handleSyncFlagPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                PopModeButtonPacket.TYPE,
//                PopModeButtonPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handlePopModeButtonPacket,
//                        ServerPayloadHandler.getInstance()::handlePopModeButtonPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                FlagSlotSetPacket.TYPE,
//                FlagSlotSetPacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleFlagSlotSetPacket,
//                        ServerPayloadHandler.getInstance()::handleFlagSlotSetPacket
//                )
//        );
//
//        // 注册 CallSeverClickPacket 用于同步滑动条状态
//        registrar.playBidirectional(
//                EnergyStoragePacket.TYPE,
//                EnergyStoragePacket.STREAM_CODEC,
//                new DirectionalPayloadHandler<>(
//                        ClientPayloadHandler.getInstance()::handleEnergyStoragePacket,
//                        ServerPayloadHandler.getInstance()::handleEnergyStoragePacket
//                )
//        );
//    }
}
