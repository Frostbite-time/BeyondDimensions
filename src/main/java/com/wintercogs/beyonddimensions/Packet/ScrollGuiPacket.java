package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ScrollGuiPacket(double offset) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<ScrollGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "scroll_gui_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<ByteBuf, ScrollGuiPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE,
                    ScrollGuiPacket::offset,
                    ScrollGuiPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

}
