package com.wintercogs.beyonddimensions.datagen.util;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;

import java.util.Set;

public abstract class BDBlockLootSubProvider extends BlockLootSubProvider
{
    protected BDBlockLootSubProvider(LootTableSubProvider.Context context)
    {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }
}