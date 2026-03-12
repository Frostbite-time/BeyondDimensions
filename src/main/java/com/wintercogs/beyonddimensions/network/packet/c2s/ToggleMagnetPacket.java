package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.client.gui.MagnetToggleType;
import com.wintercogs.beyonddimensions.common.item.BaseMachineItem;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.machine.HopperFluidMode;
import com.wintercogs.beyonddimensions.common.machine.HopperItemMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Supplier;

public record ToggleMagnetPacket(MagnetToggleType toggleType)
{
    private void toggleMagnet(Player player, List<ItemStack> itemStackList)
    {
        for (ItemStack stack : itemStackList)
        {
            if (stack.getItem() instanceof NetMagnetItem)
            {
                switch (toggleType)
                {
                    case ALL ->
                    {
                        if (BaseMachineItem.hasControlMode(stack))
                        {
                            if (BaseMachineItem.getControlModeOrDefault(stack, RedStoneControlMode.IGNORE) == RedStoneControlMode.IGNORE)
                            {
                                BaseMachineItem.setControlMode(stack, RedStoneControlMode.NOT_WORKING);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.close"));
                            }
                            else if (BaseMachineItem.getControlModeOrDefault(stack, RedStoneControlMode.IGNORE) == RedStoneControlMode.NOT_WORKING)
                            {
                                BaseMachineItem.setControlMode(stack, RedStoneControlMode.IGNORE);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.open"));
                            }
                        }
                    }
                    case ITEM ->
                    {
                        if (BaseMachineItem.hasHopperItemMode(stack))
                        {
                            if (BaseMachineItem.getHopperItemModeOrDefault(stack, HopperItemMode.ALLOW) == HopperItemMode.ALLOW)
                            {
                                BaseMachineItem.setHopperItemMode(stack, HopperItemMode.DENY);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.itemclose"));
                            }
                            else if (BaseMachineItem.getHopperItemModeOrDefault(stack, HopperItemMode.ALLOW) == HopperItemMode.DENY)
                            {
                                BaseMachineItem.setHopperItemMode(stack, HopperItemMode.ALLOW);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.itemopen"));
                            }
                        }
                    }
                    case FLUID ->
                    {
                        if (BaseMachineItem.hasHopperFluidMode(stack))
                        {
                            if (BaseMachineItem.getHopperFluidModeOrDefault(stack, HopperFluidMode.ALLOW) == HopperFluidMode.ALLOW)
                            {
                                BaseMachineItem.setHopperFluidMode(stack, HopperFluidMode.DENY);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.fluidclose"));
                            }
                            else if (BaseMachineItem.getHopperFluidModeOrDefault(stack, HopperFluidMode.ALLOW) == HopperFluidMode.DENY)
                            {
                                BaseMachineItem.setHopperFluidMode(stack, HopperFluidMode.ALLOW);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.magnet.fluidopen"));
                            }
                        }
                    }
                }
            }
        }
    }

    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player == null)
        {
            return;
        }

        toggleMagnet(player, player.getInventory().items);

        if (ModPresence.isLoaded(OtherModIds.CURIOS))
        {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
                        .stream()
                        .map(SlotResult::stack)
                        .toList();

                toggleMagnet(player, curios);
            });
        }
    }


    public static void handle(ToggleMagnetPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(ToggleMagnetPacket packet, FriendlyByteBuf buf)
    {
        buf.writeEnum(packet.toggleType());
    }

    public static ToggleMagnetPacket decode(FriendlyByteBuf buf)
    {
        return new ToggleMagnetPacket(buf.readEnum(MagnetToggleType.class));
    }
}
