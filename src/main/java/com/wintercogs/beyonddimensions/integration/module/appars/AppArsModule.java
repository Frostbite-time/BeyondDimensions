package com.wintercogs.beyonddimensions.integration.module.appars;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@BDIntegrationModule(modId = OtherModIds.ARS_ENG)
public class AppArsModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.ARS_ENG;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        BD_AE_ArsPlugin.register();
    }
}
