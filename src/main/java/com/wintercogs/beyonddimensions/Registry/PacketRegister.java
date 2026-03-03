package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Network.ClientPayloadHandler;
import com.wintercogs.beyonddimensions.Network.ServerPayloadHandler;
import com.wintercogs.beyonddimensions.Packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BeyondDimensions.MODID)
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

        registrar.playBidirectional(
                RecipeFillC2SPacket.TYPE,
                RecipeFillC2SPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleRecipeFillC2SPacket,
                        ServerPayloadHandler.getInstance()::handleRecipeFillC2SPacket
                )
        );

        registrar.playBidirectional(
                ClickTransferCraftButtonPacket.TYPE,
                ClickTransferCraftButtonPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleClickTransferCraftButtonPacket,
                        ServerPayloadHandler.getInstance()::handleClickTransferCraftButtonPacket
                )
        );

        registrar.playBidirectional(
                BatchTransferPacket.TYPE,
                BatchTransferPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleBatchTransferPacket,
                        ServerPayloadHandler.getInstance()::handleBatchTransferPacket
                )
        );

        registrar.playBidirectional(
                PickBlockFromNetPacket.TYPE,
                PickBlockFromNetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handlePickBlockFromNetPacket,
                        ServerPayloadHandler.getInstance()::handlePickBlockFromNetPacket
                )
        );

        registrar.playBidirectional(
                PutHandItemToNetPacket.TYPE,
                PutHandItemToNetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handlePutHandItemToNetPacket,
                        ServerPayloadHandler.getInstance()::handlePutHandItemToNetPacket
                )
        );

        registrar.playBidirectional(
                OrderedStackTypedSlotPacket.TYPE,
                OrderedStackTypedSlotPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleOrderedStackTypedSlotPacket,
                        ServerPayloadHandler.getInstance()::handleOrderedStackTypedSlotPacket
                )
        );

        registrar.playBidirectional(
                SetSlotDirectlyPacket.TYPE,
                SetSlotDirectlyPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleSetSlotDirectlyPacket,
                        ServerPayloadHandler.getInstance()::handleSetSlotDirectlyPacket
                )
        );

        registrar.playBidirectional(
                DisorderedSlotGroupSyncPacket.TYPE,
                DisorderedSlotGroupSyncPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleDisorderedSlotGroupSyncPacket,
                        ServerPayloadHandler.getInstance()::handleDisorderedSlotGroupSyncPacket
                )
        );

        registrar.playBidirectional(
                QuickDataTagPacket.TYPE,
                QuickDataTagPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleQuickDataTagPacket,
                        ServerPayloadHandler.getInstance()::handleQuickDataTagPacket
                )
        );

        registrar.playBidirectional(
                ToggleMagnetPacket.TYPE,
                ToggleMagnetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleToggleMagnetPacket,
                        ServerPayloadHandler.getInstance()::handleToggleMagnetPacket
                )
        );
    }
}
