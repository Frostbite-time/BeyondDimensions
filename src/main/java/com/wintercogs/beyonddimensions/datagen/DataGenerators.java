package com.wintercogs.beyonddimensions.datagen;


import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.data.loot.LootTableProvider;
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

        // 生成方块战利品表
        event.createProvider((output, lookupProvider) -> new LootTableProvider(output, Collections.emptySet(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK)), lookupProvider));
        // 生成物品和方块模型
        event.createProvider(ModModelProvider::new);

        // 生成方块、物品、流体标签
        event.createProvider(ModBlockTagProvider::new);
        event.createProvider(ModItemTagProvider::new);
        event.createProvider(ModFluidTagsProvider::new);

        // 生成配方表
        event.createProvider(ModRecipeProvider.Runner::new);

        IntegrationManager.onDatagen(event);
    }
}
