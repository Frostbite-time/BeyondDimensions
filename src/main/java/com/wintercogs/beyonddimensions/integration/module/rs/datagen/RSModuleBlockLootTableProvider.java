package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class RSModuleBlockLootTableProvider extends BDBlockLootSubProvider
{

    public RSModuleBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(registries);
    }

    @Override
    protected void generate()
    {
        dropSelf(RSModuleBlocks.RS_NET_PATHWAY.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return RSModuleBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
