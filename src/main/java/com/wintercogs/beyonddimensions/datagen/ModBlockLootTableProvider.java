package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ModBlockLootTableProvider extends BDBlockLootSubProvider
{

    protected ModBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(registries);
    }

    @Override
    protected void generate()
    {
        dropSelf(BDBlocks.NET_CONTROL.get());
        dropSelf(BDBlocks.NET_INTERFACE.get());
        dropSelf(BDBlocks.NET_PATHWAY.get());
        dropSelf(BDBlocks.NET_ENERGY_PATHWAY.get());
        dropSelf(BDBlocks.NET_TERMINAL_BLOCK.get());
        dropSelf(BDBlocks.NET_PUMP_BLOCK.get());
        dropSelf(BDBlocks.NET_HOPPER_BLOCK.get());
        dropSelf(BDBlocks.NET_FURNACE_BLOCK.get());
        dropSelf(BDBlocks.NET_BLAST_FURNACE_BLOCK.get());
        dropSelf(BDBlocks.NET_SMOKER_BLOCK.get());
        dropSelf(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return BDBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
