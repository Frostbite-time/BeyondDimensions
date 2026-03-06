package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.network.packet.both.QuickDataTagPacket;
import com.wintercogs.beyonddimensions.network.packet.both.SetSlotDirectlyPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.*;
import com.wintercogs.beyonddimensions.network.packet.s2c.DisorderedSlotGroupSyncPacket;
import com.wintercogs.beyonddimensions.network.packet.s2c.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.network.packet.s2c.PlayerPermissionInfoPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BDConstants.MODID)
public class BDPackets
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
                        OpenNetGuiPacket::handle,
                        OpenNetGuiPacket::handle
                )

        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                CallSeverClickPacket.TYPE,
                CallSeverClickPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        CallSeverClickPacket::handle,
                        CallSeverClickPacket::handle
                )
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                PlayerPermissionInfoPacket.TYPE,
                PlayerPermissionInfoPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PlayerPermissionInfoPacket::handle,
                        PlayerPermissionInfoPacket::handle
                )
        );

        // 注册 CallSeverClickPacket 用于同步滑动条状态
        registrar.playBidirectional(
                NetControlActionPacket.TYPE,
                NetControlActionPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        NetControlActionPacket::handle,
                        NetControlActionPacket::handle
                )
        );

        registrar.playBidirectional(
                RecipeFillC2SPacket.TYPE,
                RecipeFillC2SPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        RecipeFillC2SPacket::handle,
                        RecipeFillC2SPacket::handle
                )
        );

        registrar.playBidirectional(
                ClickTransferCraftButtonPacket.TYPE,
                ClickTransferCraftButtonPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClickTransferCraftButtonPacket::handle,
                        ClickTransferCraftButtonPacket::handle
                )
        );

        registrar.playBidirectional(
                BatchTransferPacket.TYPE,
                BatchTransferPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        BatchTransferPacket::handle,
                        BatchTransferPacket::handle
                )
        );

        registrar.playBidirectional(
                PickBlockFromNetPacket.TYPE,
                PickBlockFromNetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PickBlockFromNetPacket::handle,
                        PickBlockFromNetPacket::handle
                )
        );

        registrar.playBidirectional(
                PutHandItemToNetPacket.TYPE,
                PutHandItemToNetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PutHandItemToNetPacket::handle,
                        PutHandItemToNetPacket::handle
                )
        );

        registrar.playBidirectional(
                OrderedStackTypedSlotPacket.TYPE,
                OrderedStackTypedSlotPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        OrderedStackTypedSlotPacket::handle,
                        OrderedStackTypedSlotPacket::handle
                )
        );

        registrar.playBidirectional(
                SetSlotDirectlyPacket.TYPE,
                SetSlotDirectlyPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        SetSlotDirectlyPacket::handle,
                        SetSlotDirectlyPacket::handle
                )
        );

        registrar.playBidirectional(
                DisorderedSlotGroupSyncPacket.TYPE,
                DisorderedSlotGroupSyncPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        DisorderedSlotGroupSyncPacket::handle,
                        DisorderedSlotGroupSyncPacket::handle
                )
        );

        registrar.playBidirectional(
                QuickDataTagPacket.TYPE,
                QuickDataTagPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        QuickDataTagPacket::handle,
                        QuickDataTagPacket::handle
                )
        );

        registrar.playBidirectional(
                ToggleMagnetPacket.TYPE,
                ToggleMagnetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ToggleMagnetPacket::handle,
                        ToggleMagnetPacket::handle
                )
        );
    }
}
