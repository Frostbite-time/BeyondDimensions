package com.wintercogs.beyonddimensions.network.packet;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record DisorderedSlotGroupSyncPacket(int groupId, List<IStackKey<?>> keys, List<Long> newCounts,
                                            List<Long> newModifiedTime,
                                            List<Long> newInsertedTime) implements CustomPacketPayload
{
    // 定义数据包的类型 注册用
    public static final CustomPacketPayload.Type<DisorderedSlotGroupSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    BeyondDimensions.MODID,
                    "disordered_slot_group_sync_packet")); //path中不要有大写字母 仅数字 小写字母 下划线

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, DisorderedSlotGroupSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    DisorderedSlotGroupSyncPacket::groupId,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            IStackKey.STREAM_CODEC
                    ),
                    DisorderedSlotGroupSyncPacket::keys,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    DisorderedSlotGroupSyncPacket::newCounts,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    DisorderedSlotGroupSyncPacket::newModifiedTime,
                    ByteBufCodecs.collection(
                            ArrayList::new,
                            ByteBufCodecs.VAR_LONG
                    ),
                    DisorderedSlotGroupSyncPacket::newInsertedTime,
                    DisorderedSlotGroupSyncPacket::new
            );

    @Override //重写type方法，用于返回当前的TYPE
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
