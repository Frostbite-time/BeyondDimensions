package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record RecipeFillC2SPacket(List<IStackKey<?>> keys, List<Long> amount) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final Type<RecipeFillC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "recipe_fill_c2s_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeFillC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            IStackKey.STREAM_CODEC
                    ),
                    RecipeFillC2SPacket::keys,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    RecipeFillC2SPacket::amount,
                    RecipeFillC2SPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
