package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public record FlagSlotSetPacket(int index, IStackType clickStack, IStackType flagStack) implements CustomPacketPayload
{

    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<FlagSlotSetPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "flag_slot_set_pakcet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, FlagSlotSetPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    FlagSlotSetPacket::index,
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
                    FlagSlotSetPacket::clickStack,
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
                    FlagSlotSetPacket::flagStack,
                    FlagSlotSetPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

}
