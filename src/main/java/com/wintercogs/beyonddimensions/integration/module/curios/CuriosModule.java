package com.wintercogs.beyonddimensions.integration.module.curios;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@BDIntegrationModule(modId = OtherModIds.CURIOS)
public class CuriosModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.CURIOS;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        gameBus.addGenericListener(ItemStack.class, BDCuriosPlugin::registerCapabilities);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {

    }
}
