package com.wintercogs.beyonddimensions.Commands;

import com.mojang.brigadier.Command;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.TooltipHelper;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = BeyondDimensions.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientCommands
{

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event)
    {
        event.getDispatcher().register(
                Commands.literal("bdtools").then(Commands.literal("searchCache").then(Commands.literal("clear").executes(context -> {
                    TooltipHelper.clearCache();
                    context.getSource().sendSuccess(
                            () -> Component.literal("Tooltip cache cleared."),
                            false
                    );
                    return Command.SINGLE_SUCCESS;
                })))
        );
    }
}
