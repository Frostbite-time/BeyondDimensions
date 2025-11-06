package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.Block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider
{

    protected ModBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate()
    {
        dropSelf(ModBlocks.NET_CONTROL.get());
        dropSelf(ModBlocks.NET_INTERFACE.get());
        dropSelf(ModBlocks.NET_PATHWAY.get());
        dropSelf(ModBlocks.NET_ENERGY_PATHWAY.get());
        dropSelf(ModBlocks.NET_TERMINAL_BLOCK.get());
        dropSelf(ModBlocks.NET_PUMP_BLOCK.get());
        dropSelf(ModBlocks.NET_HOPPER_BLOCK.get());
        dropSelf(ModBlocks.NET_FURNACE_BLOCK.get());
        dropSelf(ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get());
        dropSelf(ModBlocks.RS_NET_PATHWAY.get());
        dropSelf(ModBlocks.ARS_SOURCE_PATHWAY.get());
        dropSelf(ModBlocks.MANA_POOL_PATHWAY.get());
        dropSelf(ModBlocks.SCHEMATICANNON_PATHWAY.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
