package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record RecipeFillC2SPacket(List<IStackKey<?>> keys, List<Long> amount,
                                  boolean compressOverflow) implements CustomPacketPayload
{
    public static final Type<RecipeFillC2SPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("recipe_fill_c2s_packet"));

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
                    ByteBufCodecs.BOOL,
                    RecipeFillC2SPacket::compressOverflow,
                    RecipeFillC2SPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();

        if (player.containerMenu instanceof DimensionsCraftMenu menu)
        {
            menu.transferRecipe(this.keys(), this.amount(), this.compressOverflow());
        }
    }

    public static void handle(final RecipeFillC2SPacket packet, final IPayloadContext context)
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
    public @NotNull Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
