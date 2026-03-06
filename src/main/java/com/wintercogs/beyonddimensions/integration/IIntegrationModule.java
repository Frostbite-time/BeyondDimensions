package com.wintercogs.beyonddimensions.integration;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public interface IIntegrationModule
{
    String modId();

    void onBootstrap(IEventBus modBus, IEventBus gameBus);

    void onCommonSetup(FMLCommonSetupEvent event);

    default void onItemCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {

    }

    default void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {

    }

    default void onDatagen(GatherDataEvent.Client event)
    {

    }
}

