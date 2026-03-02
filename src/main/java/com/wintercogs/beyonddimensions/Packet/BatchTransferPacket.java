package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BatchTransferPacket(KeyAmount clickStack, boolean dirToStorage) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<BatchTransferPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "batch_transfer_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, BatchTransferPacket> STREAM_CODEC =
            StreamCodec.composite(
                    KeyAmount.STREAM_CODEC,
                    BatchTransferPacket::clickStack,
                    ByteBufCodecs.BOOL,
                    BatchTransferPacket::dirToStorage,
                    BatchTransferPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
