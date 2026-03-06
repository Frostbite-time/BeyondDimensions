package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

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
        blockWithItem(ModBlocks.DIMENSIONAL_CONNECT_BLOCK);
        blockWithItem(ModBlocks.RS_NET_PATHWAY);
        blockWithItem(ModBlocks.ARS_SOURCE_PATHWAY);
        // obj方块 自编写json 仅注册物品
        simpleBlockItem(ModBlocks.NET_TERMINAL_BLOCK.get(), models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID, "net_terminal_block")));
        simpleBlockItem(ModBlocks.NET_PUMP_BLOCK.get(), models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID, "net_pump_block")));
        simpleBlockItem(ModBlocks.NET_HOPPER_BLOCK.get(), models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID, "net_hopper_block")));
        simpleBlockItem(ModBlocks.NET_FURNACE_BLOCK.get(), models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID, "net_furnace_block")));
        simpleBlockItem(ModBlocks.MANA_POOL_PATHWAY.get(), models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana_pool_pathway")));
        simpleBlockItem(ModBlocks.SCHEMATICANNON_PATHWAY.get(), models().getExistingFile(ResourceLocation.tryBuild(BeyondDimensions.MODID, "schematicannon_pathway")));
    }

    private void blockWithItem(RegistryObject<Block> deferredBlock)
    {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}
