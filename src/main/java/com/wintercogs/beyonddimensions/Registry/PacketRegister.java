package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Network.ClientPayloadHandler;
import com.wintercogs.beyonddimensions.Network.ServerPayloadHandler;
import com.wintercogs.beyonddimensions.Packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.MOD)
public class PacketRegister
{

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event)
    {
        //设置当前网络版本
        final PayloadRegistrar registrar = event.registrar("1");

        // 注册OpenNetGuiPacket 用于打开当前角色绑定的维度网络GUI
        registrar.playBidirectional(
                OpenNetGuiPacket.TYPE,
                OpenNetGuiPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleOpenNetGuiPacket,
                        ServerPayloadHandler.getInstance()::handleOpenNetGuiPacket
                )

        );

        // 注册 ItemStoragePacket 用于同步滑动条状态
        registrar.playBidirectional(
                StoragePacket.TYPE,
                StoragePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleItemStoragePacket,
                        ServerPayloadHandler.getInstance()::handleItemStoragePacket
                )
        );

        // 注册 CallSeverStoragePacket 用于同步滑动条状态
        registrar.playBidirectional(
                CallSeverStoragePacket.TYPE,
                CallSeverStoragePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleCallSeverStoragePacket,
                        ServerPayloadHandler.getInstance()::handleCallSeverStoragePacket
                )
        );

        // 注册 SyncItemStoragePacket 用于同步滑动条状态
        registrar.playBidirectional(
                SyncStoragePacket.TYPE,
                SyncStoragePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleSyncItemStoragePacket,
                        ServerPayloadHandler.getInstance()::handleSyncItemStoragePacket
                )
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                CallSeverClickPacket.TYPE,
                CallSeverClickPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleCallSeverClickPacket,
                        ServerPayloadHandler.getInstance()::handleCallSeverClickPacket
                )
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                CallServerPlayerInfoPacket.TYPE,
                CallServerPlayerInfoPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleCallServerPlayerInfoPacket,
                        ServerPayloadHandler.getInstance()::handleCallServerPlayerInfoPacket
                )
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                PlayerPermissionInfoPacket.TYPE,
                PlayerPermissionInfoPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handlePlayerPermissionInfoPacket,
                        ServerPayloadHandler.getInstance()::handlePlayerPermissionInfoPacket
                )
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                NetControlActionPacket.TYPE,
                NetControlActionPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleNetControlActionPacket,
                        ServerPayloadHandler.getInstance()::handleNetControlActionPacket
                )
        );
    }
}
