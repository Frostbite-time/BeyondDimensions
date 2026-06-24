package com.wintercogs.beyonddimensions.integration.module.botania;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.botania.block.entity.ManaPoolPathwayBlockEntity;
import com.wintercogs.beyonddimensions.integration.module.botania.datagen.BotaniaModuleBlockLootTableProvider;
import com.wintercogs.beyonddimensions.integration.module.botania.datagen.BotaniaModuleBlockStateProvider;
import com.wintercogs.beyonddimensions.integration.module.botania.datagen.BotaniaModuleRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.botania.eventlistener.BotaniaModuleDataPackSyncListener;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlockEntities;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlocks;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaHandlerWrapper;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaStackKey;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaStackTypedHandler;
import com.wintercogs.beyonddimensions.integration.module.botania.storage.ManaUnifiedStorageHandler;
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

@BDIntegrationModule(modId = OtherModIds.BOTANIA)
public class BotaniaModule implements IIntegrationModule
{
    @Override
    public String modId()
    {
        return OtherModIds.BOTANIA;
    }

    @Override
    public void onBootstrap(IEventBus modBus, IEventBus gameBus)
    {
        BotaniaModuleBlocks.register(modBus);
        BotaniaModuleBlockEntities.register(modBus);

        modBus.addListener(ManaPoolPathwayBlockEntity::registerCapability);
        modBus.addListener(BDBotaniaPlugin::registerCapability);
        gameBus.addListener(BotaniaModuleDataPackSyncListener::onDataPackSync);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StackKeyRegistry.registerType(ManaStackKey.INSTANCE);
        CapabilityHelper.BlockCapabilityMap.put(ManaStackKey.ID, BotaniaCapabilityCompat.manaReceiver());
        CapabilityHelper.ItemCapabilityMap.put(ManaStackKey.ID, BotaniaCapabilityCompat.manaItem());
        CapabilityHelper.registerUSHandler(ManaStackKey.INSTANCE, ManaUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(ManaStackKey.INSTANCE, ManaStackTypedHandler::new);
        StackHandlerWrapperHelper.stackWrappers.put(ManaStackKey.ID, ManaHandlerWrapper::new);

        // 能力交互黑名单（魔力手镜）
        BDBotaniaPlugin.registerItemCapBlackList();
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(BotaniaModuleBlocks.MANA_POOL_PATHWAY);
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(BotaniaModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider)
        {
            @Override
            public @NotNull String getName()
            {
                return "BeyondDimensions BotaniaModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new BotaniaModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new BotaniaModuleRecipeProvider(packOutput, lookupProvider));
    }

    @Override
    public void onBlockTagDatagen(HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, BotaniaModuleBlocks.MANA_POOL_PATHWAY.getId());
    }
}
