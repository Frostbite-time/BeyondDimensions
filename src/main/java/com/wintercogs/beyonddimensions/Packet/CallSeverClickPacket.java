package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallSeverClickPacket(int slotIndex , IStackType clickItem, int button, boolean shiftDown) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final Type<CallSeverClickPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "call_sever_click_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, CallSeverClickPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    CallSeverClickPacket::slotIndex,
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
