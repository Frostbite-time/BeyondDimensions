package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.network.packet.c2s.*;
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

        registrar.playBidirectional(
                OpenNetGuiPacket.TYPE,
                OpenNetGuiPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        OpenNetGuiPacket::handle,
                        OpenNetGuiPacket::handle
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
                ToggleMagnetPacket.TYPE,
                ToggleMagnetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ToggleMagnetPacket::handle,
                        ToggleMagnetPacket::handle
                )
        );

        registrar.playBidirectional(
                OpenMagnetGuiPacket.TYPE,
                OpenMagnetGuiPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        OpenMagnetGuiPacket::handle,
                        OpenMagnetGuiPacket::handle
                )
        );

        registrar.playBidirectional(
                OpenPrimaryNetSwitcherPacket.TYPE,
                OpenPrimaryNetSwitcherPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        OpenPrimaryNetSwitcherPacket::handle,
                        OpenPrimaryNetSwitcherPacket::handle
                )
        );

        registrar.playBidirectional(
                PrimaryNetSwitchActionPacket.TYPE,
                PrimaryNetSwitchActionPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PrimaryNetSwitchActionPacket::handle,
                        PrimaryNetSwitchActionPacket::handle
                )
        );

        registrar.playBidirectional(
                RenameNetPacket.TYPE,
                RenameNetPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        RenameNetPacket::handle,
                        RenameNetPacket::handle
                )
        );
    }
}
