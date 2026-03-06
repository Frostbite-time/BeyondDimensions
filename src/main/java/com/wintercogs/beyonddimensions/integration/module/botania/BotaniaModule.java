package com.wintercogs.beyonddimensions.integration.module.botania;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.integration.BDIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
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
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import vazkii.botania.api.BotaniaForgeCapabilities;

import java.util.Collections;
import java.util.List;

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
        gameBus.addGenericListener(BlockEntity.class, BD_BotaniaPlugin::attachBlockEntityCaps);
        gameBus.addListener(BotaniaModuleDataPackSyncListener::onDataPackSync);
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event)
    {
        StackKeyRegistry.registerType(ManaStackKey.INSTANCE);
        CapabilityHelper.BlockCapabilityMap.put(ManaStackKey.ID, BotaniaForgeCapabilities.MANA_RECEIVER);
        CapabilityHelper.ItemCapabilityMap.put(ManaStackKey.ID, BotaniaForgeCapabilities.MANA_ITEM);
        CapabilityHelper.registerUSHandler(ManaStackKey.INSTANCE, ManaUnifiedStorageHandler::new);
        CapabilityHelper.registerStackTypedHandler(ManaStackKey.INSTANCE, ManaStackTypedHandler::new);
        StackHandlerWrapperHelper.stackWrappers.put(ManaStackKey.ID, ManaHandlerWrapper::new);
        BD_BotaniaPlugin.registerItemCapBlackList();
    }

    @Override
    public void onBlockCreativeTabCollect(CreativeModeTab.ItemDisplayParameters displayParameters, CreativeModeTab.Output output)
    {
        output.accept(BotaniaModuleBlocks.MANA_POOL_PATHWAY.get());
    }

    @Override
    public void onBlockTagDatagen(net.minecraft.core.HolderLookup.Provider provider, BlockTagAppender appender)
    {
        appender.addOptional(BlockTags.MINEABLE_WITH_PICKAXE, BotaniaModuleBlocks.MANA_POOL_PATHWAY.getId());
    }

    @Override
    public void onDatagen(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(BotaniaModuleBlockLootTableProvider::new, LootContextParamSets.BLOCK)))
        {
            @Override
            public String getName()
            {
                return "BeyondDimensions BotaniaModule LootTable Provider";
            }
        });
        generator.addProvider(event.includeClient(), new BotaniaModuleBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeServer(), new BotaniaModuleRecipeProvider(packOutput));
    }
}
