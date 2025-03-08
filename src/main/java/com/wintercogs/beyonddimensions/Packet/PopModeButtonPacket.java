package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PopModeButtonPacket(boolean popMode) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final Type<PopModeButtonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "pop_mode_button_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, PopModeButtonPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PopModeButtonPacket::popMode,
                    PopModeButtonPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
