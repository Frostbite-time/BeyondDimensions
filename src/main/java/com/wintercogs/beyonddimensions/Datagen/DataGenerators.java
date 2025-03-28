//package com.wintercogs.beyonddimensions.Datagen;
//
//
//import com.mojang.logging.LogUtils;
//import com.wintercogs.beyonddimensions.BeyondDimensions;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.data.DataGenerator;
//import net.minecraft.data.PackOutput;
//import net.minecraft.data.loot.LootTableProvider;
//import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
//import net.minecraftforge.common.data.BlockTagsProvider;
//import net.minecraftforge.common.data.ExistingFileHelper;
//import net.minecraftforge.data.event.GatherDataEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import org.slf4j.Logger;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//
//@Mod.EventBusSubscriber(modid = BeyondDimensions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//public class DataGenerators
//{
//    public static final Logger LOGGER = LogUtils.getLogger();
//
//    @SubscribeEvent
//    public static void gatherData(GatherDataEvent event)
//    {
//        LOGGER.info("数据生成启动");
//        DataGenerator generator = event.getGenerator();
//        PackOutput packOutput = generator.getPackOutput();
//        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
//        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
//
//        // 生成方块战利品表
//        generator.addProvider(event.includeServer(),new LootTableProvider(packOutput, Collections.emptySet(),
//                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK))));
//        // 生成物品和方块模型
//        generator.addProvider(event.includeClient(),new ModItemModelProvider(packOutput,existingFileHelper));
//        generator.addProvider(event.includeClient(),new ModBlockStateProvider(packOutput,existingFileHelper));
//
//        // 生成方块和物品标签
//        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(packOutput,lookupProvider,existingFileHelper);
//        generator.addProvider(event.includeServer(), blockTagsProvider);
//        generator.addProvider(event.includeServer(), new ModItemTagProvider(packOutput,lookupProvider,blockTagsProvider.contentsGetter(),existingFileHelper));
//
//        // 生成配方表
//        generator.addProvider(event.includeServer(),new ModRecipeProvider(packOutput));
//
//    }
//}
