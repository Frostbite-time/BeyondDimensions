package com.wintercogs.beyonddimensions.integration.module.appflux;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@BDIntegrationModule(modId = OtherModIds.APPFLUX)
public class AppFluxModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.APPFLUX;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        BD_AEFluxPlugin.register();
    }
}
