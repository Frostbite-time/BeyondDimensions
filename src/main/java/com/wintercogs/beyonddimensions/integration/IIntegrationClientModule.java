package com.wintercogs.beyonddimensions.integration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public interface IIntegrationClientModule
{
    String modId();

    void onBootstrapClient(IEventBus modBus, IEventBus gameBus);

    void onClientSetup(FMLClientSetupEvent event);
}

