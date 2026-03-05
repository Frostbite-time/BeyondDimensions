package com.wintercogs.beyonddimensions.integration.curios;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationModIds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@BDIntegrationModule(modId = IntegrationModIds.CURIOS)
public class CuriosIntegrationModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return IntegrationModIds.CURIOS;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        modBus.addListener(BDCuriosPlugin::registerCapabilities);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {

    }
}
