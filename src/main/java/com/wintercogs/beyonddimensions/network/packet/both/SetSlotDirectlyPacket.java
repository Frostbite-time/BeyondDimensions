package com.wintercogs.beyonddimensions.network.packet.both;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 此记录的处理会调用对应slot的setStackDirectly
// 不会有数据校验
// 因此，请仅在绝对需要setStackDirectly再重写实现（如标记槽位）
public record SetSlotDirectlyPacket(int slotId, KeyAmount stack) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<SetSlotDirectlyPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "set_slot_directly_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, SetSlotDirectlyPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SetSlotDirectlyPacket::slotId,
                    KeyAmount.STREAM_CODEC,
                    SetSlotDirectlyPacket::stack,
                    SetSlotDirectlyPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
