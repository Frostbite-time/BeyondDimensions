package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.network.packet.c2s.*;
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

    // 定义网络通道。数据包按注册顺序编号，种类或顺序改变时提高版本，不同版本的两端不会互连
    private static final String PROTOCOL_VERSION = "2";
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

    static
    {
        registerC2S(OpenNetGuiPacket.class, OpenNetGuiPacket::encode, OpenNetGuiPacket::decode, OpenNetGuiPacket::handle);
        registerC2S(PickBlockFromNetPacket.class, PickBlockFromNetPacket::encode, PickBlockFromNetPacket::decode, PickBlockFromNetPacket::handle);
        registerC2S(PutHandItemToNetPacket.class, PutHandItemToNetPacket::encode, PutHandItemToNetPacket::decode, PutHandItemToNetPacket::handle);
        registerC2S(ToggleMagnetPacket.class, ToggleMagnetPacket::encode, ToggleMagnetPacket::decode, ToggleMagnetPacket::handle);
        registerC2S(OpenMagnetGuiPacket.class, OpenMagnetGuiPacket::encode, OpenMagnetGuiPacket::decode, OpenMagnetGuiPacket::handle);
        registerC2S(OpenPrimaryNetSwitcherPacket.class, OpenPrimaryNetSwitcherPacket::encode, OpenPrimaryNetSwitcherPacket::decode, OpenPrimaryNetSwitcherPacket::handle);
        registerC2S(PrimaryNetSwitchActionPacket.class, PrimaryNetSwitchActionPacket::encode, PrimaryNetSwitchActionPacket::decode, PrimaryNetSwitchActionPacket::handle);
        registerC2S(RenameNetPacket.class, RenameNetPacket::encode, RenameNetPacket::decode, RenameNetPacket::handle);
        registerC2S(ConfigCardPacket.class, ConfigCardPacket::encode, ConfigCardPacket::decode, ConfigCardPacket::handle);
    }
}
