package com.wintercogs.beyonddimensions.integration.module.create.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import com.wintercogs.beyonddimensions.integration.module.create.init.CreateModuleBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public class CreateModuleBlockLootTableProvider extends BDBlockLootSubProvider
{
    public CreateModuleBlockLootTableProvider()
    {
        super();
    }

    @Override
    protected void generate()
    {
        dropSelf(CreateModuleBlocks.SCHEMATICANNON_PATHWAY.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return CreateModuleBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}
