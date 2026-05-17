package com.wintercogs.beyonddimensions.network.packet.both;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 用于双端互相同步，无验证的数据包，不要用它传递重要信息
 * <p>一般用于传递方块或者物品的设置信息，且仅利用菜单进行读写，以防止被伪造数据包远程修改</p>
 * <p>服务端中每tick验证并发送同步，客户端中仅在点击按钮等确定性修改后发送同步</p>
 */
public record QuickDataTagPacket(CompoundTag tag) implements CustomPacketPayload
{
    public static final Type<QuickDataTagPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("quick_data_tag_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuickDataTagPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    QuickDataTagPacket::tag,
                    QuickDataTagPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {
        Player player = context.player();
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            menu.readQuickDataTag(this.tag());
        }
    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            menu.readQuickDataTag(this.tag());
        }
    }

    public static void handle(final QuickDataTagPacket packet, final IPayloadContext context)
    {
        if (packet != null)
        {
            PacketFlow direction = context.flow();
            if (direction == PacketFlow.CLIENTBOUND)
            {
                context.enqueueWork(() -> packet.handleInClient(context));
            }
            else if (direction == PacketFlow.SERVERBOUND)
            {
                context.enqueueWork(() -> packet.handleInServer(context));
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
