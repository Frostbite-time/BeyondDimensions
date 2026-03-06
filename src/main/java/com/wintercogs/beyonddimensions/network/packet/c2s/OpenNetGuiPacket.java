package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenNetGuiPacket(String uuid, NetMenuType target) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<OpenNetGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "open_net_gui_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<ByteBuf, OpenNetGuiPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    OpenNetGuiPacket::uuid,
                    new StreamCodec<ByteBuf, NetMenuType>()
                    {
                        @Override
                        public void encode(ByteBuf buf, NetMenuType netMenuType)
                        {
                            Utf8String.write(buf, netMenuType.toString(), 32000);
                        }

                        @Override
                        public NetMenuType decode(ByteBuf buf)
                        {
                            return NetMenuType.valueOf(Utf8String.read(buf, 32000));
                        }
                    },
                    OpenNetGuiPacket::target,
                    OpenNetGuiPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
