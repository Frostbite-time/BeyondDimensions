package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Utf8String;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record NetControlActionPacket(UUID receiver, NetControlAction action) implements CustomPacketPayload
{
    public static final StreamCodec<ByteBuf, NetControlAction> NET_CONTROL_ACTION_STREAM_CODEC = netControlActionStreamCodec();

    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<NetControlActionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "net_control_action_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<ByteBuf, NetControlActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC,
                    NetControlActionPacket::receiver,
                    NET_CONTROL_ACTION_STREAM_CODEC,
                    NetControlActionPacket::action,
                    NetControlActionPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    static StreamCodec<ByteBuf, NetControlAction> netControlActionStreamCodec()
    {
        return new StreamCodec<ByteBuf, NetControlAction>() {
            public NetControlAction decode(ByteBuf buf) {
                return NetControlAction.valueOf(Utf8String.read(buf,32000));
            }

            public void encode(ByteBuf buf, NetControlAction permissionlevel) {
                Utf8String.write(buf, permissionlevel.toString(), 32000);
            }
        };
    }

}
