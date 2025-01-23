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

        // 注册ScrollGuiPacket 用于滑动UI
        registrar.playBidirectional(
                ScrollGuiPacket.TYPE,
                ScrollGuiPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleScrollGuiPacket,
                        ServerPayloadHandler.getInstance()::handleScrollGuiPacket
                )
        );

        // 注册ScrollGuiPacket 用于同步翻页、搜索、排序等操作对槽位的影响
        registrar.playBidirectional(
                SlotIndexPacket.TYPE,
                SlotIndexPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleSlotIndexPacket,
                        ServerPayloadHandler.getInstance()::handleSlotIndexPacket
                )
        );

        // 注册 SearchAndButtonGuiPacket 用于从本地同步搜索和按钮状态
        registrar.playBidirectional(
                SearchAndButtonGuiPacket.TYPE,
                SearchAndButtonGuiPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleSearchAndButtonGuiPacket,
                        ServerPayloadHandler.getInstance()::handleSearchAndButtonGuiPacket
                )
        );

        // 注册 ScrollLinedataPacket 用于同步滑动条状态
        registrar.playBidirectional(
                ScrollLinedataPacket.TYPE,
                ScrollLinedataPacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleScrollLinedataPacket,
                        ServerPayloadHandler.getInstance()::handleScrollLinedataPacket
                )
        );

        // 注册 ItemStoragePacket 用于同步滑动条状态
        registrar.playBidirectional(
                ItemStoragePacket.TYPE,
                ItemStoragePacket.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandler.getInstance()::handleItemStoragePacket,
                        ServerPayloadHandler.getInstance()::handleItemStoragePacket
                )
        );
    }
}
