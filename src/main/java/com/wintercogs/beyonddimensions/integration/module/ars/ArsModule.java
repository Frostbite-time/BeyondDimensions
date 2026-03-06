package com.wintercogs.beyonddimensions.integration.module.ars;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ars.datagen.ArsModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.datagen.ArsModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.datagen.ArsModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlocks;
import com.wintercogs.beyonddimensions.integration.module.ars.storage.SourceHandlerWrapper;
import com.wintercogs.beyonddimensions.integration.module.ars.storage.SourceStackKey;
import com.wintercogs.beyonddimensions.integration.module.ars.storage.SourceStackTypedHandler;
import com.wintercogs.beyonddimensions.integration.module.ars.storage.SourceUnifiedStorageHandler;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Collections;
import java.util.List;

@BDIntegrationModule(modId = OtherModIds.ARS_NOUVEAU)
public class ArsModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.ARS_NOUVEAU;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        ArsModuleBlocks.register(modBus);
        ArsModuleBlockEntities.register(modBus);
        BDArsCaps.registerCapability(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StackKeyRegistry.registerType(SourceStackKey.INSTANCE);
        CapabilityHelper.BlockCapabilityMap.put(SourceStackKey.ID, BDArsCaps.SOURCE_CAP);
        CapabilityHelper.ItemCapabilityMap.put(SourceStackKey.ID, BDArsCaps.SOURCE_CAP);
        CapabilityHelper.registerUSHandler(SourceStackKey.INSTANCE, SourceUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(SourceStackKey.INSTANCE, SourceStackTypedHandler::new);
        StackHandlerWrapperHelper.stackWrappers.put(SourceStackKey.ID, SourceHandlerWrapper::new);
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(ArsModuleBlocks.ARS_SOURCE_PATHWAY.get());
    }

    @Override
    public void onBlockTagDatagen(net.minecraft.core.HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, ArsModuleBlocks.ARS_SOURCE_PATHWAY.getId());
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ArsModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)))
        {
            @Override
            public String getName()
            {
                return "BeyondDimensions ArsModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new ArsModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new ArsModuleRecipeProvider(packOutput));
    }
}
