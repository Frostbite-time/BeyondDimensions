package com.wintercogs.beyonddimensions.integration.module.rs;

import com.refinedmods.refinedstorage.api.storage.StorageType;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.simibubi.create.AllTags;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.rs.datagen.RSModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.datagen.RSModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.datagen.RSModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import com.wintercogs.beyonddimensions.integration.module.rs.storage.BD_RS120ExternalStorageProviderFluids;
import com.wintercogs.beyonddimensions.integration.module.rs.storage.BD_RS120ExternalStorageProviderItems;
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

@BDIntegrationModule(modId = OtherModIds.REFINED_STORAGE)
public class RSModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.REFINED_STORAGE;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        RSModuleBlocks.register(modBus);
        RSModuleBlockEntities.register(modBus);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        API.instance().addExternalStorageProvider(StorageType.ITEM, new BD_RS120ExternalStorageProviderItems());
        API.instance().addExternalStorageProvider(StorageType.FLUID, new BD_RS120ExternalStorageProviderFluids());
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(RSModuleBlocks.RS_NET_PATHWAY.get());
    }

    @Override
    public void onBlockTagDatagen(net.minecraft.core.HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, RSModuleBlocks.RS_NET_PATHWAY.getId());
        appender.addOptional(AllTags.AllBlockTags.NON_MOVABLE.tag, RSModuleBlocks.RS_NET_PATHWAY.getId());
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(RSModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)))
        {
            @Override
            public String getName()
            {
                return "BeyondDimensions RSModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new RSModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new RSModuleRecipeProvider(packOutput));
    }
}
