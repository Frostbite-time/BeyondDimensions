package com.wintercogs.beyonddimensions.integration.module.ars.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ArsModuleBlockLootTableProvider extends BDBlockLootSubProvider
{

    public ArsModuleBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(registries);
    }

    @Override
    protected void generate()
    {
        dropSelf(ArsModuleBlocks.ARS_SOURCE_PATHWAY.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return ArsModuleBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
