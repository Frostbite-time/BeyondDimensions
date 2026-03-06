package com.wintercogs.beyonddimensions.datagen.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;

import java.util.Set;

public abstract class BDBlockLootSubProvider extends BlockLootSubProvider
{
    protected BDBlockLootSubProvider(HolderLookup.Provider registries)
    {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }
}
