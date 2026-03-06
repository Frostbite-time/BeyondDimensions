package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.network.packet.both.QuickDataTagPacket;
import com.wintercogs.beyonddimensions.network.packet.both.SetSlotDirectlyPacket;
import com.wintercogs.beyonddimensions.network.packet.c2s.*;
import com.wintercogs.beyonddimensions.network.packet.s2c.DisorderedSlotGroupSyncPacket;
import com.wintercogs.beyonddimensions.network.packet.s2c.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.network.packet.s2c.PlayerPermissionInfoPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BDPackets
{

    // 定义网络通道
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(BDConstants.MODID, "simple_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 1;

    private static <MSG> void registerC2S(Class<MSG> type,
                                           BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf> encoder,
                                           Function<net.minecraft.network.FriendlyByteBuf, MSG> decoder,
                                           BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler)
    {
        INSTANCE.registerMessage(packetId++, type, encoder, decoder, handler, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <MSG> void registerS2C(Class<MSG> type,
                                           BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf> encoder,
                                           Function<net.minecraft.network.FriendlyByteBuf, MSG> decoder,
                                           BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler)
    {
        INSTANCE.registerMessage(packetId++, type, encoder, decoder, handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static <MSG> void registerBoth(Class<MSG> type,
                                           BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf> encoder,
                                           Function<net.minecraft.network.FriendlyByteBuf, MSG> decoder,
                                           BiConsumer<MSG, java.util.function.Supplier<NetworkEvent.Context>> handler)
    {
        INSTANCE.registerMessage(packetId++, type, encoder, decoder, handler);
    }

    static
    {
        registerC2S(OpenNetGuiPacket.class, OpenNetGuiPacket::encode, OpenNetGuiPacket::decode, OpenNetGuiPacket::handle);
        registerC2S(CallSeverClickPacket.class, CallSeverClickPacket::encode, CallSeverClickPacket::decode, CallSeverClickPacket::handle);
        registerC2S(NetControlActionPacket.class, NetControlActionPacket::encode, NetControlActionPacket::decode, NetControlActionPacket::handle);
        registerC2S(RecipeFillC2SPacket.class, RecipeFillC2SPacket::encode, RecipeFillC2SPacket::decode, RecipeFillC2SPacket::handle);
        registerC2S(ClickTransferCraftButtonPacket.class, ClickTransferCraftButtonPacket::encode, ClickTransferCraftButtonPacket::decode, ClickTransferCraftButtonPacket::handle);
        registerC2S(BatchTransferPacket.class, BatchTransferPacket::encode, BatchTransferPacket::decode, BatchTransferPacket::handle);
        registerC2S(PickBlockFromNetPacket.class, PickBlockFromNetPacket::encode, PickBlockFromNetPacket::decode, PickBlockFromNetPacket::handle);
        registerC2S(PutHandItemToNetPacket.class, PutHandItemToNetPacket::encode, PutHandItemToNetPacket::decode, PutHandItemToNetPacket::handle);
        registerC2S(ToggleMagnetPacket.class, ToggleMagnetPacket::encode, ToggleMagnetPacket::decode, ToggleMagnetPacket::handle);

        registerS2C(PlayerPermissionInfoPacket.class, PlayerPermissionInfoPacket::encode, PlayerPermissionInfoPacket::decode, PlayerPermissionInfoPacket::handle);
        registerS2C(DisorderedSlotGroupSyncPacket.class, DisorderedSlotGroupSyncPacket::encode, DisorderedSlotGroupSyncPacket::decode, DisorderedSlotGroupSyncPacket::handle);
        registerS2C(OrderedStackTypedSlotPacket.class, OrderedStackTypedSlotPacket::encode, OrderedStackTypedSlotPacket::decode, OrderedStackTypedSlotPacket::handle);

        registerBoth(SetSlotDirectlyPacket.class, SetSlotDirectlyPacket::encode, SetSlotDirectlyPacket::decode, SetSlotDirectlyPacket::handle);
        registerBoth(QuickDataTagPacket.class, QuickDataTagPacket::encode, QuickDataTagPacket::decode, QuickDataTagPacket::handle);
    }
}
