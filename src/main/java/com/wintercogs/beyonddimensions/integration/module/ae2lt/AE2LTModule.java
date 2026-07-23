package com.wintercogs.beyonddimensions.integration.module.ae2lt;

import com.moakiee.ae2lt.api.AE2LTCapabilities;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.block.entity.LightningPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.datagen.AE2LTModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.datagen.AE2LTModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.datagen.AE2LTModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.init.AE2LTModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.init.AE2LTModuleBlocks;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.storage.LightningHandlerWrapper;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.storage.LightningStackKey;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.storage.LightningStackTypedHandler;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.storage.LightningUnifiedStorageHandler;
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

@BDIntegrationModule(modId = OtherModIds.AE2_LIGHTNING_TECH)
public class AE2LTModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.AE2_LIGHTNING_TECH;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        AE2LTModuleBlocks.register(modBus);
        AE2LTModuleBlockEntities.register(modBus);
        modBus.addListener(LightningPathwayBlockEntity::registerCapability);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StackKeyRegistry.registerType(LightningStackKey.HIGH_VOLTAGE);
        CapabilityHelper.BlockCapabilityMap.put(LightningStackKey.ID, AE2LTCapabilities.LIGHTNING_ENERGY_BLOCK);
        CapabilityHelper.registerUSHandler(LightningStackKey.HIGH_VOLTAGE, LightningUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(LightningStackKey.HIGH_VOLTAGE, LightningStackTypedHandler::new);
        StackHandlerWrapperHelper.stackWrappers.put(LightningStackKey.ID, LightningHandlerWrapper::new);
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output)
    {
        output.accept(AE2LTModuleBlocks.LIGHTNING_PATHWAY);
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> registries = event.getLookupProvider();
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(
                        AE2LTModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)), registries)
        {
            @Override
            public @NotNull String getName()
            {
                return "BeyondDimensions AE2LT Module LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new AE2LTModuleBlockStateProvider(output, files));
        generator.addProvider(event.includeServer(), new AE2LTModuleRecipeProvider(output, registries));
    }

    @Override
    public void onBlockTagDatagen(HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, AE2LTModuleBlocks.LIGHTNING_PATHWAY.getId());
    }
}
