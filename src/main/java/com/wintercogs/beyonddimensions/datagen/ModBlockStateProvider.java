package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.datagen.util.BDBlockStateProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

public class ModBlockStateProvider extends BDBlockStateProvider
{

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, exFileHelper);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions BlockState Provider";
    }

    @Override
    protected void registerStatesAndModels()
    {
        blockWithItem(BDBlocks.NET_CONTROL);
        blockWithItem(BDBlocks.NET_INTERFACE);
        blockWithItem(BDBlocks.NET_PATHWAY);
        blockWithItem(BDBlocks.NET_ENERGY_PATHWAY);
        blockWithItem(BDBlocks.DIMENSIONAL_CONNECT_BLOCK);
        // obj方块 自编写json 仅注册物品
        simpleBlockItem(BDBlocks.NET_TERMINAL_BLOCK.get(), models().getExistingFile(BeyondDimensions.makeId("net_terminal_block")));
        simpleBlockItem(BDBlocks.NET_PUMP_BLOCK.get(), models().getExistingFile(BeyondDimensions.makeId("net_pump_block")));
        simpleBlockItem(BDBlocks.NET_HOPPER_BLOCK.get(), models().getExistingFile(BeyondDimensions.makeId("net_hopper_block")));
        simpleBlockItem(BDBlocks.NET_FURNACE_BLOCK.get(), models().getExistingFile(BeyondDimensions.makeId("net_furnace_block")));
        simpleBlockItem(BDBlocks.MANA_POOL_PATHWAY.get(), models().getExistingFile(BeyondDimensions.makeId("mana_pool_pathway")));
        simpleBlockItem(BDBlocks.SCHEMATICANNON_PATHWAY.get(), models().getExistingFile(BeyondDimensions.makeId("schematicannon_pathway")));
    }
}
