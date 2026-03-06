package com.wintercogs.beyonddimensions.integration.module.mekanism;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.GasStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.InfusionStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.PigmentStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.ordered.SlurryStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.GasUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.InfusionUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.PigmentUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.unordered.SlurryUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.*;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.GasStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.InfusionStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.PigmentStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.SlurryStackKey;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

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
        StackKeyRegistry.registerType(GasStackKey.EMPTY);
        StackKeyRegistry.registerType(InfusionStackKey.EMPTY);
        StackKeyRegistry.registerType(PigmentStackKey.EMPTY);
        StackKeyRegistry.registerType(SlurryStackKey.EMPTY);
        // 注册化学品方块能力
        CapabilityHelper.BlockCapabilityMap.put(GasStackKey.ID, mekanism.common.capabilities.Capabilities.GAS_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(InfusionStackKey.ID, mekanism.common.capabilities.Capabilities.INFUSION_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(PigmentStackKey.ID, mekanism.common.capabilities.Capabilities.PIGMENT_HANDLER);
        CapabilityHelper.BlockCapabilityMap.put(SlurryStackKey.ID, mekanism.common.capabilities.Capabilities.SLURRY_HANDLER);
        // 注册化学品物品能力
        CapabilityHelper.ItemCapabilityMap.put(GasStackKey.ID, mekanism.common.capabilities.Capabilities.GAS_HANDLER);
        CapabilityHelper.ItemCapabilityMap.put(InfusionStackKey.ID, mekanism.common.capabilities.Capabilities.INFUSION_HANDLER);
        CapabilityHelper.ItemCapabilityMap.put(PigmentStackKey.ID, mekanism.common.capabilities.Capabilities.PIGMENT_HANDLER);
        CapabilityHelper.ItemCapabilityMap.put(SlurryStackKey.ID, mekanism.common.capabilities.Capabilities.SLURRY_HANDLER);

        // 注册分化包装
        CapabilityHelper.registerUSHandler(GasStackKey.EMPTY, GasUnifiedStorageHandler::new);
        CapabilityHelper.registerUSHandler(InfusionStackKey.EMPTY, InfusionUnifiedStorageHandler::new);
        CapabilityHelper.registerUSHandler(PigmentStackKey.EMPTY, PigmentUnifiedStorageHandler::new);
        CapabilityHelper.registerUSHandler(SlurryStackKey.EMPTY, SlurryUnifiedStorageHandler::new);

        // 注册存储分化包装
        CapabilityHelper.registerStackTypedHandler(GasStackKey.EMPTY, GasStackTypedHandler::new);
        CapabilityHelper.registerStackTypedHandler(InfusionStackKey.EMPTY, InfusionStackTypedHandler::new);
        CapabilityHelper.registerStackTypedHandler(PigmentStackKey.EMPTY, PigmentStackTypedHandler::new);
        CapabilityHelper.registerStackTypedHandler(SlurryStackKey.EMPTY, SlurryStackTypedHandler::new);

        // 注册堆叠处理包装
        StackHandlerWrapperHelper.stackWrappers.put(GasStackKey.ID, GasHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(InfusionStackKey.ID, InfusionHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(PigmentStackKey.ID, PigmentHandlerWrapper::new);
        StackHandlerWrapperHelper.stackWrappers.put(SlurryStackKey.ID, SlurryHandlerWrapper::new);
    }
}
