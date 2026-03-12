package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMagnetGuiPacket() implements CustomPacketPayload {

    public static final Type<OpenMagnetGuiPacket> TYPE = new Type<>(BeyondDimensions.makeId("open_magnet_gui_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMagnetGuiPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf o, OpenMagnetGuiPacket openMagnetGuiPaket)
        {
        }

        @Override
        public OpenMagnetGuiPacket decode(RegistryFriendlyByteBuf registryFriendlyByteBuf)
        {
            return new OpenMagnetGuiPacket();
        }
    };


    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);

        if (net == null)
            return;

        ItemStack itemstack = InventoryHelper.findItemInPlayerInventory(player, BDItems.NET_MAGNET_ITEM.get());
        if (itemstack == null)
            return;

        player.openMenu(new SimpleMenuProvider((containerId, inv, ServerPlayer) ->
                new NetMagnetMenu(containerId, inv, itemstack),
                Component.translatable("menu.title.beyonddimensions.magnet_menu"))
        );
    }

    public static void handle(final OpenMagnetGuiPacket packet, final IPayloadContext context)
    {
        if (packet == null)
            return;

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

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
