package com.wintercogs.beyonddimensions.network.Packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.item.BaseMachineItem;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Supplier;

public record ToggleMagnetPacket()
{
    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();

        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.getItem() instanceof NetMagnetItem)
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
        }

        if (BeyondDimensions.CuriosLoaded)
        {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
                        .stream()
                        .map(SlotResult::stack)
                        .toList();

                for (ItemStack stack : curios)
                {
                    if (stack.getItem() instanceof NetMagnetItem)
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
                }
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

    }

    public static ToggleMagnetPacket decode(FriendlyByteBuf buf)
    {
        return new ToggleMagnetPacket();
    }
}
