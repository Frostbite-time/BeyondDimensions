package com.wintercogs.beyonddimensions.integration.module.ae2lt.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.init.AE2LTModuleBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class AE2LTModuleBlockLootTableProvider extends BDBlockLootSubProvider
{
    public AE2LTModuleBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(registries);
    }

    @Override
    protected void generate()
    {
        dropSelf(AE2LTModuleBlocks.LIGHTNING_PATHWAY.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return AE2LTModuleBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
