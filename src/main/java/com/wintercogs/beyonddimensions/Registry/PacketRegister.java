package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.network.Packet.both.CallSeverClickPacket;
import com.wintercogs.beyonddimensions.network.Packet.both.QuickDataTagPacket;
import com.wintercogs.beyonddimensions.network.Packet.both.SetSlotDirectlyPacket;
import com.wintercogs.beyonddimensions.network.Packet.c2s.*;
import com.wintercogs.beyonddimensions.network.Packet.s2c.DisorderedSlotGroupSyncPacket;
import com.wintercogs.beyonddimensions.network.Packet.s2c.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.network.Packet.s2c.PlayerPermissionInfoPacket;
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
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "simple_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 1;

    static
    {
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
                RecipeFillC2SPacket.class,
                RecipeFillC2SPacket::encode,
                RecipeFillC2SPacket::decode,
                RecipeFillC2SPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                ClickTransferCraftButtonPacket.class,
                ClickTransferCraftButtonPacket::encode,
                ClickTransferCraftButtonPacket::decode,
                ClickTransferCraftButtonPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                BatchTransferPacket.class,
                BatchTransferPacket::encode,
                BatchTransferPacket::decode,
                BatchTransferPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                PickBlockFromNetPacket.class,
                PickBlockFromNetPacket::encode,
                PickBlockFromNetPacket::decode,
                PickBlockFromNetPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                PutHandItemToNetPacket.class,
                PutHandItemToNetPacket::encode,
                PutHandItemToNetPacket::decode,
                PutHandItemToNetPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                DisorderedSlotGroupSyncPacket.class,
                DisorderedSlotGroupSyncPacket::encode,
                DisorderedSlotGroupSyncPacket::decode,
                DisorderedSlotGroupSyncPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                OrderedStackTypedSlotPacket.class,
                OrderedStackTypedSlotPacket::encode,
                OrderedStackTypedSlotPacket::decode,
                OrderedStackTypedSlotPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                SetSlotDirectlyPacket.class,
                SetSlotDirectlyPacket::encode,
                SetSlotDirectlyPacket::decode,
                SetSlotDirectlyPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                QuickDataTagPacket.class,
                QuickDataTagPacket::encode,
                QuickDataTagPacket::decode,
                QuickDataTagPacket::handle
        );

        INSTANCE.registerMessage(
                packetId++,
                ToggleMagnetPacket.class,
                ToggleMagnetPacket::encode,
                ToggleMagnetPacket::decode,
                ToggleMagnetPacket::handle
        );
    }
}
