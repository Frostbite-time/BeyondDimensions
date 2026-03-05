package com.wintercogs.beyonddimensions.integration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public interface IIntegrationModule
{
    String modId();

    void onBootstrap(IEventBus modBus, IEventBus gameBus);

    void onCommonSetup(FMLCommonSetupEvent event);
}

