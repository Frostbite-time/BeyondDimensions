package com.wintercogs.beyonddimensions.Datagen;


import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Datagen.helpers.DataProviderEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

@EventBusSubscriber(modid = BeyondDimensions.MODID)
public class DataGenerators
{
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final List<DataProviderEntry> additionalProviders = new ArrayList<>();

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event)
    {
        LOGGER.info("数据生成启动");
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 生成方块战利品表
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        // 生成物品和方块模型
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));

        // 生成方块、物品、流体标签
        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
        generator.addProvider(event.includeServer(), new ModFluidTagsProvider(packOutput, lookupProvider, existingFileHelper));

        // 生成配方表
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput, lookupProvider));

        // 添加额外provider，如各个集成模块下的数据生成
        for(DataProviderEntry entry : additionalProviders)
        {
            boolean run = entry.condition().test(event);
            DataProvider provider = entry.factory().apply(event);
            if(provider != null)
            {
                generator.addProvider(run, provider);
            }
        }
    }

    public static void addAdditionalProvider(Predicate<GatherDataEvent> condition, Function<GatherDataEvent, DataProvider> factory)
    {
        additionalProviders.add(new DataProviderEntry(condition, factory));
    }
}
