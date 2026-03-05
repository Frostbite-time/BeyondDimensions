package com.wintercogs.beyonddimensions.integration.mekanism;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ChemicalStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.ChemicalHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ChemicalUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
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
        // 注册化学品堆叠
        StackKeyRegistry.registerType(ChemicalStackKey.EMPTY);
        // 注册化学品方块能力
        CapabilityHelper.BlockCapabilityMap.put(ChemicalStackKey.ID, mekanism.common.capabilities.Capabilities.CHEMICAL.block());
        // 注册化学品物品能力
        CapabilityHelper.ItemCapabilityMap.put(ChemicalStackKey.ID, mekanism.common.capabilities.Capabilities.CHEMICAL.item());
        // 注册分化包装
        CapabilityHelper.registerUSHandler(ChemicalStackKey.EMPTY, ChemicalUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(ChemicalStackKey.EMPTY, ChemicalStackTypedHandler::new);
        // 注册堆叠处理包装
        StackHandlerWrapperHelper.stackWrappers.put(ChemicalStackKey.ID, ChemicalHandlerWrapper::new);
    }
}
