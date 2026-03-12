package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.MagnetToggleType;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.machine.HopperFluidMode;
import com.wintercogs.beyonddimensions.common.machine.HopperItemMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record ToggleMagnetPacket(MagnetToggleType toggleType) implements CustomPacketPayload
{
    public static final Type<ToggleMagnetPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("toggle_magnet_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPacket> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPacket>()
    {
        @Override
        public ToggleMagnetPacket decode(RegistryFriendlyByteBuf registryFriendlyByteBuf)
        {
            return new ToggleMagnetPacket(registryFriendlyByteBuf.readEnum(MagnetToggleType.class));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ToggleMagnetPacket toggleMagnetPacket)
        {
            buf.writeEnum(toggleMagnetPacket.toggleType);
        }
    };

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void toggleMagnet(Player player, List<ItemStack> itemStackList)
    {
        for (ItemStack stack : itemStackList)
        {
            if (!(stack.getItem() instanceof NetMagnetItem))
            {
                continue;
            }

            switch (toggleType)
            {
                case ALL -> {
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
                case ITEM -> {
                    if (stack.has(BDDataComponents.HOPPER_ITEM_MODE))
                    {
                        if (stack.get(BDDataComponents.HOPPER_ITEM_MODE) == HopperItemMode.ALLOW)
                        {
                            stack.set(BDDataComponents.HOPPER_ITEM_MODE, HopperItemMode.DENY);
                            player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.itemclose"), false);
                        }
                        else if (stack.get(BDDataComponents.HOPPER_ITEM_MODE) == HopperItemMode.DENY)
                        {
                            stack.set(BDDataComponents.HOPPER_ITEM_MODE, HopperItemMode.ALLOW);
                            player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.itemopen"), false);
                        }
                    }
                }
                case FLUID -> {
                    if (stack.has(BDDataComponents.HOPPER_FLUID_MODE))
                    {
                        if (stack.get(BDDataComponents.HOPPER_FLUID_MODE) == HopperFluidMode.ALLOW)
                        {
                            stack.set(BDDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.DENY);
                            player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.fluidclose"), false);
                        }
                        else if (stack.get(BDDataComponents.HOPPER_FLUID_MODE) == HopperFluidMode.DENY)
                        {
                            stack.set(BDDataComponents.HOPPER_FLUID_MODE, HopperFluidMode.ALLOW);
                            player.displayClientMessage(Component.translatable("msg.beyonddimensions.magnet.fluidopen"), false);
                        }
                    }
                }
            }
        }
    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();

        toggleMagnet(player, player.getInventory().getNonEquipmentItems());

        if (ModPresence.isLoaded(OtherModIds.CURIOS))
        {
//            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
//                List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
//                        .stream()
//                        .map(SlotResult::stack)
//                        .toList();
//
//                toggleMagnet(player, curios);
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
