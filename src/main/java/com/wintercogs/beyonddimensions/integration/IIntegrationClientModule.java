package com.wintercogs.beyonddimensions.integration;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public interface IIntegrationClientModule
{
    String modId();

    void onBootstrapClient(IEventBus modBus, IEventBus gameBus);

    void onClientSetup(FMLClientSetupEvent event);
}
