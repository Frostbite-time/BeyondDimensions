package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BatchTransferPacket(IStackType clickStack, boolean dirToStorage) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<BatchTransferPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "batch_transfer_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, BatchTransferPacket> STREAM_CODEC =
            StreamCodec.composite(
                    new StreamCodec<RegistryFriendlyByteBuf, IStackType>()
                    {

                        @Override
                        public void encode(RegistryFriendlyByteBuf buf, IStackType stackType)
                        {
                            stackType.serialize(buf);
                        }

                        @Override
                        public IStackType decode(RegistryFriendlyByteBuf byteBuf)
                        {
                            return IStackType.deserializeCommon(byteBuf);
                        }
                    },
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
