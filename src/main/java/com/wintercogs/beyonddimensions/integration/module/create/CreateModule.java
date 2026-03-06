package com.wintercogs.beyonddimensions.integration.module.create;

import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.create.block.entity.SchematicannonPathWayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.create.datagen.CreateModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.create.datagen.CreateModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.create.datagen.CreateModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@BDIntegrationModule(modId = OtherModIds.CREATE)
public class CreateModule implements IIntegrationModule
{

    @Override
    public String modId()
    {
        return OtherModIds.CREATE;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        CreateModuleBlocks.register(modBus);
        CreateModuleBlockEntities.register(modBus);
        modBus.addListener(SchematicannonPathWayBlockEntity::registerCapability);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {

    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(CreateModuleBlocks.SCHEMATICANNON_PATHWAY);
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(CreateModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider)
        {
            @Override
            public @NotNull String getName()
            {
                return "BeyondDimensions CreateModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new CreateModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new CreateModuleRecipeProvider(packOutput, lookupProvider));
    }

    @Override
    public void onBlockTagDatagen(HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, CreateModuleBlocks.SCHEMATICANNON_PATHWAY.getId());
    }
}
