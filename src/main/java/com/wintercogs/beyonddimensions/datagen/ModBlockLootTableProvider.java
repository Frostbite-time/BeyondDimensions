package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockLootTableProvider extends BDBlockLootSubProvider
{


    protected ModBlockLootTableProvider()
    {
        super();
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
        dropSelf(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get());
        dropSelf(BDBlocks.ARS_SOURCE_PATHWAY.get());
        dropSelf(BDBlocks.MANA_POOL_PATHWAY.get());
        dropSelf(BDBlocks.SCHEMATICANNON_PATHWAY.get());

        BDFluids.ALL.forEach(e -> add((LiquidBlock) BDFluids.XP_FLUID.block().get(), LootTable.lootTable()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks()
    {
        return BDBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}
