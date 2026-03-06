package com.wintercogs.beyonddimensions.integration.module.botania.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public class BotaniaModuleBlockLootTableProvider extends BDBlockLootSubProvider
{
    public BotaniaModuleBlockLootTableProvider()
    {
        super();
    }

    @Override
    protected void generate()
    {
        dropSelf(BotaniaModuleBlocks.MANA_POOL_PATHWAY.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return BotaniaModuleBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}
