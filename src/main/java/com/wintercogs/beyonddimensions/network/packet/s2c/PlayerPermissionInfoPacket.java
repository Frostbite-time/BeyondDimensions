package com.wintercogs.beyonddimensions.network.packet.s2c;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.UUID;

public record PlayerPermissionInfoPacket(HashMap<UUID, PlayerPermissionInfo> infoMap) implements CustomPacketPayload
{
    public static final Type<PlayerPermissionInfoPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("player_permission_info_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPermissionInfoPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(
                            HashMap::new,
                            UUIDUtil.STREAM_CODEC,
                            PlayerPermissionInfo.STREAM_CODEC
                    ),
                    PlayerPermissionInfoPacket::infoMap,
                    PlayerPermissionInfoPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {
        Player player = context.player();
        NetControlMenu menu;
        if (!(player.containerMenu instanceof NetControlMenu))
        {
            return;
        }
        menu = (NetControlMenu) player.containerMenu;
        menu.loadPlayerInfo(this.infoMap());
    }

    private void handleInServer(final IPayloadContext context)
    {

    }

    public static void handle(final PlayerPermissionInfoPacket packet, final IPayloadContext context)
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
