package com.wintercogs.beyonddimensions.network.packet.both;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 用于双端互相同步，无验证的数据包，不要用它传递重要信息
// 一般用于传递方块或者物品的设置信息，且仅利用菜单进行读写，以防止被伪造数据包远程修改
// 服务端中每tick验证并发送同步，客户端中仅在点击按钮等确定性修改后发送同步
public record QuickDataTagPacket(CompoundTag tag) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<QuickDataTagPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "quick_data_tag_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, QuickDataTagPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    QuickDataTagPacket::tag,
                    QuickDataTagPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
