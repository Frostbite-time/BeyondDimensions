package com.wintercogs.beyonddimensions.integration.curios;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.ModIds;
import net.neoforged.bus.api.IEventBus;

@BDIntegrationModule(modId = ModIds.CURIOS)
public class CuriosIntegrationModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return ModIds.CURIOS;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        modBus.addListener(BD_CuriosPlugin::registerCapabilities);
    }
}
