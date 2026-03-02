package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

public record PutHandItemToNetPacket(InteractionHand hand) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<PutHandItemToNetPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "put_hand_item_to_net_packet"));

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, PutHandItemToNetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.map(
                            InteractionHand::valueOf,
                            InteractionHand::name
                    ),
                    PutHandItemToNetPacket::hand,
                    PutHandItemToNetPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
