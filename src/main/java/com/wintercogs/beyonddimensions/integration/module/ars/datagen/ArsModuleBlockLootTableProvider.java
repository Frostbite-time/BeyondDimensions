package com.wintercogs.beyonddimensions.integration.module.ars.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public class ArsModuleBlockLootTableProvider extends BDBlockLootSubProvider
{
    public ArsModuleBlockLootTableProvider()
    {
        super();
    }

    @Override
    protected void generate()
    {
        dropSelf(ArsModuleBlocks.ARS_SOURCE_PATHWAY.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return ArsModuleBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}
