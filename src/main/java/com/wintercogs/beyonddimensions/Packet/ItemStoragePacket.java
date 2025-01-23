package com.wintercogs.beyonddimensions.Packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.StoredItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public record ItemStoragePacket(ArrayList<StoredItemStack> storedItemStacks) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<ItemStoragePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "item_storage_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStoragePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            StoredItemStack.STREAM_CODEC
                    ),
                    ItemStoragePacket::storedItemStacks,
                    ItemStoragePacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
