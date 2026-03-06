package com.wintercogs.beyonddimensions.client.command;

import com.mojang.brigadier.Command;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.util.TooltipHelper;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
