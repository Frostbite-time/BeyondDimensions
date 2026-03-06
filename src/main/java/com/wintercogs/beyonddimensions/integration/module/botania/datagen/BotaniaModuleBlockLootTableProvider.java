package com.wintercogs.beyonddimensions.integration.module.botania.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class BotaniaModuleBlockLootTableProvider extends BDBlockLootSubProvider
{

    public BotaniaModuleBlockLootTableProvider(HolderLookup.Provider registries)
    {
        super(registries);
    }

    @Override
    protected void generate()
    {
        dropSelf(BotaniaModuleBlocks.MANA_POOL_PATHWAY.get());
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return BotaniaModuleBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}
