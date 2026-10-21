package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockLootSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

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
        dropSelf(BDBlocks.NET_BLAST_FURNACE_BLOCK.get());
        dropSelf(BDBlocks.NET_SMOKER_BLOCK.get());
        dropSelf(BDBlocks.NET_STOCK_EMITTER.get());
        dropSelf(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get());
        // 1.20.1 复制水的方块属性时不带「无战利品表」，流体方块要给空表
        BDFluids.ALL.forEach(fluid -> add(fluid.block().get(), LootTable.lootTable()));
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks()
    {
        return BDBlocks.BLOCKS.getEntries().stream().flatMap(RegistryObject::stream)::iterator;
    }
}
