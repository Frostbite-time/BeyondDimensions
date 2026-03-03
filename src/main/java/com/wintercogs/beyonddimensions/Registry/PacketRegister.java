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
                ServerPayloadHandler.getInstance()::handleOpenNetGuiPacket,
                ClientPayloadHandler.getInstance()::handleOpenNetGuiPacket
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                CallSeverClickPacket.TYPE,
                CallSeverClickPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleCallSeverClickPacket,
                ClientPayloadHandler.getInstance()::handleCallSeverClickPacket
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                PlayerPermissionInfoPacket.TYPE,
                PlayerPermissionInfoPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handlePlayerPermissionInfoPacket,
                ClientPayloadHandler.getInstance()::handlePlayerPermissionInfoPacket
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                NetControlActionPacket.TYPE,
                NetControlActionPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleNetControlActionPacket,
                ClientPayloadHandler.getInstance()::handleNetControlActionPacket
        );

        registrar.playBidirectional(
                RecipeFillC2SPacket.TYPE,
                RecipeFillC2SPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleRecipeFillC2SPacket,
                ClientPayloadHandler.getInstance()::handleRecipeFillC2SPacket
        );

        registrar.playBidirectional(
                ClickTransferCraftButtonPacket.TYPE,
                ClickTransferCraftButtonPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleClickTransferCraftButtonPacket,
                ClientPayloadHandler.getInstance()::handleClickTransferCraftButtonPacket
        );

        registrar.playBidirectional(
                BatchTransferPacket.TYPE,
                BatchTransferPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleBatchTransferPacket,
                ClientPayloadHandler.getInstance()::handleBatchTransferPacket
        );

        registrar.playBidirectional(
                PickBlockFromNetPacket.TYPE,
                PickBlockFromNetPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handlePickBlockFromNetPacket,
                ClientPayloadHandler.getInstance()::handlePickBlockFromNetPacket
        );

        registrar.playBidirectional(
                PutHandItemToNetPacket.TYPE,
                PutHandItemToNetPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handlePutHandItemToNetPacket,
                ClientPayloadHandler.getInstance()::handlePutHandItemToNetPacket
        );

        registrar.playBidirectional(
                OrderedStackTypedSlotPacket.TYPE,
                OrderedStackTypedSlotPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleOrderedStackTypedSlotPacket,
                ClientPayloadHandler.getInstance()::handleOrderedStackTypedSlotPacket
        );

        registrar.playBidirectional(
                SetSlotDirectlyPacket.TYPE,
                SetSlotDirectlyPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleSetSlotDirectlyPacket,
                ClientPayloadHandler.getInstance()::handleSetSlotDirectlyPacket
        );

        registrar.playBidirectional(
                DisorderedSlotGroupSyncPacket.TYPE,
                DisorderedSlotGroupSyncPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleDisorderedSlotGroupSyncPacket,
                ClientPayloadHandler.getInstance()::handleDisorderedSlotGroupSyncPacket
        );

        registrar.playBidirectional(
                QuickDataTagPacket.TYPE,
                QuickDataTagPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleQuickDataTagPacket,
                ClientPayloadHandler.getInstance()::handleQuickDataTagPacket
        );

        registrar.playBidirectional(
                ToggleMagnetPacket.TYPE,
                ToggleMagnetPacket.STREAM_CODEC,
                ServerPayloadHandler.getInstance()::handleToggleMagnetPacket,
                ClientPayloadHandler.getInstance()::handleToggleMagnetPacket
        );
    }
}
