package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallSeverStoragePacket() implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final Type<CallSeverStoragePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "call_sever_storage_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, CallSeverStoragePacket> STREAM_CODEC =
            new StreamCodec<RegistryFriendlyByteBuf, CallSeverStoragePacket>() {
                @Override
                public CallSeverStoragePacket decode(RegistryFriendlyByteBuf buf) {
                    // 无数据需要解码，返回一个空的 CallSeverStoragePacket
                    return new CallSeverStoragePacket();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CallSeverStoragePacket packet) {
                    // 不需要写入任何数据
                }
            };

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
