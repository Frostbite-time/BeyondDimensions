package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.resources.ResourceLocation;


public record CallServerPlayerInfoPacket() implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<CallServerPlayerInfoPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "call_server_player_info_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, CallServerPlayerInfoPacket> STREAM_CODEC =
            new StreamCodec<RegistryFriendlyByteBuf, CallServerPlayerInfoPacket>() {
                @Override
                public CallServerPlayerInfoPacket decode(RegistryFriendlyByteBuf buf) {
                    // 无数据需要解码，返回一个空的 CallSeverStoragePacket
                    return new CallServerPlayerInfoPacket();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CallServerPlayerInfoPacket packet) {
                    // 不需要写入任何数据
                }
            };


    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
