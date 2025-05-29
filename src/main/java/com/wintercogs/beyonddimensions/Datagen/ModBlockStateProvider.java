package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider
{

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, BeyondDimensions.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels()
    {
        blockWithItem(ModBlocks.NET_CONTROL);
        blockWithItem(ModBlocks.NET_INTERFACE);
        blockWithItem(ModBlocks.NET_PATHWAY);
        blockWithItem(ModBlocks.NET_ENERGY_PATHWAY);
        // obj方块 自编写json，仅注册物品
        simpleBlockItem(ModBlocks.NET_TERMINAL_BLOCK.get(),models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID,"net_terminal_block")));
    }

    private void blockWithItem(DeferredBlock<?> deferredBlock)
    {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
