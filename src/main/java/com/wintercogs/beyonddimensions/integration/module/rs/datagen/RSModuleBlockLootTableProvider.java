package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public class RSModuleBlockLootTableProvider extends BDBlockLootSubProvider
{
    public RSModuleBlockLootTableProvider()
    {
        super();
    }

    @Override
    protected void generate()
    {
        dropSelf(RSModuleBlocks.RS_NET_PATHWAY.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return RSModuleBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}
