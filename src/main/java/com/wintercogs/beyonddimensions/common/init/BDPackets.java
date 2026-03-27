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
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = BDConstants.MODID)
public class BDPackets
{

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event)
    {
        //设置当前网络版本
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playBidirectional(
                OpenNetGuiPacket.TYPE,
                OpenNetGuiPacket.STREAM_CODEC,
                OpenNetGuiPacket::handle,
                OpenNetGuiPacket::handle
        );

        registrar.playBidirectional(
                CallSeverClickPacket.TYPE,
                CallSeverClickPacket.STREAM_CODEC,
                CallSeverClickPacket::handle,
                CallSeverClickPacket::handle
        );

        registrar.playBidirectional(
                PlayerPermissionInfoPacket.TYPE,
                PlayerPermissionInfoPacket.STREAM_CODEC,
                PlayerPermissionInfoPacket::handle,
                PlayerPermissionInfoPacket::handle
        );

        registrar.playBidirectional(
                NetControlActionPacket.TYPE,
                NetControlActionPacket.STREAM_CODEC,
                NetControlActionPacket::handle,
                NetControlActionPacket::handle
        );

        registrar.playBidirectional(
                RecipeFillC2SPacket.TYPE,
                RecipeFillC2SPacket.STREAM_CODEC,
                RecipeFillC2SPacket::handle,
                RecipeFillC2SPacket::handle
        );

        registrar.playBidirectional(
                ClickTransferCraftButtonPacket.TYPE,
                ClickTransferCraftButtonPacket.STREAM_CODEC,
                ClickTransferCraftButtonPacket::handle,
                ClickTransferCraftButtonPacket::handle
        );

        registrar.playBidirectional(
                BatchTransferPacket.TYPE,
                BatchTransferPacket.STREAM_CODEC,
                BatchTransferPacket::handle,
                BatchTransferPacket::handle
        );

        registrar.playBidirectional(
                PickBlockFromNetPacket.TYPE,
                PickBlockFromNetPacket.STREAM_CODEC,
                PickBlockFromNetPacket::handle,
                PickBlockFromNetPacket::handle
        );

        registrar.playBidirectional(
                PutHandItemToNetPacket.TYPE,
                PutHandItemToNetPacket.STREAM_CODEC,
                PutHandItemToNetPacket::handle,
                PutHandItemToNetPacket::handle
        );

        registrar.playBidirectional(
                OrderedStackTypedSlotPacket.TYPE,
                OrderedStackTypedSlotPacket.STREAM_CODEC,
                OrderedStackTypedSlotPacket::handle,
                OrderedStackTypedSlotPacket::handle
        );

        registrar.playBidirectional(
                SetSlotDirectlyPacket.TYPE,
                SetSlotDirectlyPacket.STREAM_CODEC,
                SetSlotDirectlyPacket::handle,
                SetSlotDirectlyPacket::handle
        );

        registrar.playBidirectional(
                DisorderedSlotGroupSyncPacket.TYPE,
                DisorderedSlotGroupSyncPacket.STREAM_CODEC,
                DisorderedSlotGroupSyncPacket::handle,
                DisorderedSlotGroupSyncPacket::handle
        );

        registrar.playBidirectional(
                QuickDataTagPacket.TYPE,
                QuickDataTagPacket.STREAM_CODEC,
                QuickDataTagPacket::handle,
                QuickDataTagPacket::handle
        );

        registrar.playBidirectional(
                ToggleMagnetPacket.TYPE,
                ToggleMagnetPacket.STREAM_CODEC,
                ToggleMagnetPacket::handle,
                ToggleMagnetPacket::handle
        );

        registrar.playBidirectional(
                OpenMagnetGuiPacket.TYPE,
                OpenMagnetGuiPacket.STREAM_CODEC,
                OpenMagnetGuiPacket::handle,
                OpenMagnetGuiPacket::handle
        );

        registrar.playBidirectional(
                OpenPrimaryNetSwitcherPacket.TYPE,
                OpenPrimaryNetSwitcherPacket.STREAM_CODEC,
                OpenPrimaryNetSwitcherPacket::handle,
                OpenPrimaryNetSwitcherPacket::handle
        );

        registrar.playBidirectional(
                PrimaryNetSwitchActionPacket.TYPE,
                PrimaryNetSwitchActionPacket.STREAM_CODEC,
                PrimaryNetSwitchActionPacket::handle,
                PrimaryNetSwitchActionPacket::handle
        );
    }
}
