package com.wintercogs.beyonddimensions.integration.polymorph;

import com.wintercogs.beyonddimensions.integration.BDIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationClientModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@BDIntegrationClientModule(modId = OtherModIds.POLYMORPH)
public class PolymorphModuleClient implements IIntegrationClientModule
{
    @Override
    public String modId()
    {
        return OtherModIds.POLYMORPH;
    }

    @Override
    public void onBootstrapClient(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event)
    {
        PolymorphPlug.register();
    }

}
