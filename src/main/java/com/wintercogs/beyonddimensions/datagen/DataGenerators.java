package com.wintercogs.beyonddimensions.datagen;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = BDConstants.MODID)
public class DataGenerators
{
    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event)
    {
        LOGGER.info("数据生成启动");

        // createReloadableRegistryObjects 在整个 GatherDataEvent 中只能调用一次：
        // 每次调用都会向 DataGenerator 注册一个固定名为 "Registries for reloadable"
        // 的 provider，重复调用会抛出 Duplicate provider: Registries for reloadable。
        // 因此战利品表、配方表以及各集成模块的条目必须合并到同一个 RegistrySetBuilder 中。
        RegistrySetBuilder registrySetBuilder = new RegistrySetBuilder()
                // 生成方块战利品表
                .add(Registries.LOOT_TABLE, new LootTableProvider(Collections.emptySet(),
                        List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK))))
                // 生成配方表（含配方解锁进度）
                .add(RecipeProvider.asBootstrap(ModRecipeProvider::new));

        // 各集成模块向同一个 builder 追加条目
        IntegrationManager.addReloadableRegistryEntries(registrySetBuilder);

        event.createReloadableRegistryObjects(registrySetBuilder);

        // 生成物品和方块模型
        event.createProvider(ModModelProvider::new);

        // 生成方块、物品、流体标签
        event.createProvider(ModBlockTagProvider::new);
        event.createProvider(ModItemTagProvider::new);
        event.createProvider(ModFluidTagsProvider::new);

        IntegrationManager.onDatagen(event);
    }
}