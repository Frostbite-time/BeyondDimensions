package com.wintercogs.beyonddimensions.network.packet;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CallSeverClickPacket(int slotIndex, KeyAmount clickItem, int button,
                                   boolean shiftDown) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final Type<CallSeverClickPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    BDConstants.MODID,
                    "call_sever_click_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, CallSeverClickPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CallSeverClickPacket::slotIndex,
                    KeyAmount.STREAM_CODEC,
                    CallSeverClickPacket::clickItem,
                    ByteBufCodecs.VAR_INT,
                    CallSeverClickPacket::button,
                    ByteBufCodecs.BOOL,
                    CallSeverClickPacket::shiftDown,
                    CallSeverClickPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
