package com.wintercogs.beyonddimensions.integration.module.mekanism;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.mekanism.storage.ChemicalHandlerWrapper;
import com.wintercogs.beyonddimensions.integration.module.mekanism.storage.ChemicalStackKey;
import com.wintercogs.beyonddimensions.integration.module.mekanism.storage.ChemicalStackTypedHandler;
import com.wintercogs.beyonddimensions.integration.module.mekanism.storage.ChemicalUnifiedStorageHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@BDIntegrationModule(modId = OtherModIds.MEKANISM)
public class MekModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.MEKANISM;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {

    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StackKeyRegistry.registerType(ChemicalStackKey.EMPTY);

        CapabilityHelper.BlockCapabilityMap.put(ChemicalStackKey.ID, mekanism.common.capabilities.Capabilities.CHEMICAL.block());
        CapabilityHelper.ItemCapabilityMap.put(ChemicalStackKey.ID, mekanism.common.capabilities.Capabilities.CHEMICAL.item());

        CapabilityHelper.registerUSHandler(ChemicalStackKey.EMPTY, ChemicalUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(ChemicalStackKey.EMPTY, ChemicalStackTypedHandler::new);

        StackHandlerWrapperHelper.stackWrappers.put(ChemicalStackKey.ID, ChemicalHandlerWrapper::new);
    }
}
