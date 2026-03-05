package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.integration.ModIds;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleMagnetPacket() implements CustomPacketPayload
{
    public static final Type<ToggleMagnetPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("toggle_magnet_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPacket> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPacket>()
    {
        @Override
        public ToggleMagnetPacket decode(RegistryFriendlyByteBuf registryFriendlyByteBuf)
        {
            return new ToggleMagnetPacket();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ToggleMagnetPacket toggleMagnetPacket)
        {

        }
    };

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();

        for (ItemStack stack : player.getInventory().getNonEquipmentItems())
        {
            if (stack.getItem() instanceof NetMagnetItem)
            {
                if (stack.has(BDDataComponents.CONTROL_MODE))
                {
                    if (stack.get(BDDataComponents.CONTROL_MODE) == RedStoneControlMode.IGNORE)
                    {
                        stack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.NOT_WORKING);
                        player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.close"), false);
                    }
                    else if (stack.get(BDDataComponents.CONTROL_MODE) == RedStoneControlMode.NOT_WORKING)
                    {
                        stack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
                        player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.open"), false);
                    }
                }
            }
        }

        if (ModPresence.isLoaded(ModIds.CURIOS))
        {
//            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
//                List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
//                        .stream()
//                        .map(SlotResult::stack)
//                        .toList();
//
//                for (ItemStack stack : curios)
//                {
//                    if (stack.getItem() instanceof NetMagnetItem)
//                    {
//                        if (stack.has(BDDataComponents.CONTROL_MODE))
//                        {
//                            if (stack.get(BDDataComponents.CONTROL_MODE) == RedStoneControlMode.IGNORE)
//                            {
//                                stack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.NOT_WORKING);
//                                player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.close"), false);
//                            }
//                            else if (stack.get(BDDataComponents.CONTROL_MODE) == RedStoneControlMode.NOT_WORKING)
//                            {
//                                stack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
//                                player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.open"), false);
//                            }
//                        }
//                    }
//                }
//            });
        }
    }

    public static void handle(final ToggleMagnetPacket packet, final IPayloadContext context)
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
