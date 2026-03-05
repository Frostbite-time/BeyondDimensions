package com.wintercogs.beyonddimensions.integration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public interface IIntegrationModule
{
    String modId();

    default void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    default void onCommonSetup(FMLCommonSetupEvent event)
    {
    }
}
