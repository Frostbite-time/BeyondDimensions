package com.wintercogs.beyonddimensions.integration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public interface IIntegrationClientModule
{
    String modId();

    default void onBootstrapClient(IEventBus modBus, IEventBus gameBus)
    {
    }

    default void onClientSetup(FMLClientSetupEvent event)
    {
    }
}
