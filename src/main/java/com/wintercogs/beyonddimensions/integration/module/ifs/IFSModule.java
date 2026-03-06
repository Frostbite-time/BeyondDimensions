package com.wintercogs.beyonddimensions.integration.module.ifs;

import com.buuz135.industrialforegoingsouls.capabilities.SoulCapabilities;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ifs.datagen.IFSModuleItemModelProvider;
import com.wintercogs.beyonddimensions.integration.module.ifs.datagen.IFSModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.ifs.init.IFSModuleItems;
import com.wintercogs.beyonddimensions.integration.module.ifs.item.WardenSoulTagItem;
import com.wintercogs.beyonddimensions.integration.module.ifs.storage.WardenSoulHandlerWrapper;
import com.wintercogs.beyonddimensions.integration.module.ifs.storage.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.integration.module.ifs.storage.WardenSoulStackTypedHandler;
import com.wintercogs.beyonddimensions.integration.module.ifs.storage.WardenSoulUnifiedStorageHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@BDIntegrationModule(modId = OtherModIds.INDUSTRIAL_FOREGOING_SOULS)
public class IFSModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.INDUSTRIAL_FOREGOING_SOULS;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        IFSModuleItems.register(modBus);
        modBus.addListener(WardenSoulTagItem::registerCapability);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        // 注册监守者之魂
        StackKeyRegistry.registerType(WardenSoulStackKey.INSTANCE);
        CapabilityHelper.BlockCapabilityMap.put(WardenSoulStackKey.ID, SoulCapabilities.BLOCK);
        // 此处为自定义物品能力，因为原模组未提供物品能力
        CapabilityHelper.ItemCapabilityMap.put(WardenSoulStackKey.ID, BDSoulCaps.ITEM);
        // 注册分化包装
        CapabilityHelper.registerUSHandler(WardenSoulStackKey.INSTANCE, WardenSoulUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(WardenSoulStackKey.INSTANCE, WardenSoulStackTypedHandler::new);
        // 注册堆叠处理包装
        StackHandlerWrapperHelper.stackWrappers.put(WardenSoulStackKey.ID, WardenSoulHandlerWrapper::new);
    }

    @Override
    public void onItemCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(IFSModuleItems.WARDEN_SOUL_TAG_ITEM);
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new IFSModuleItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new IFSModuleRecipeProvider(packOutput, lookupProvider));
    }
}
