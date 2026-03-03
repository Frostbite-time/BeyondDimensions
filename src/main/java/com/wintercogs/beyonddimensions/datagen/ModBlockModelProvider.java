package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

public class ModBlockModelProvider extends ModelProvider
{

    public ModBlockModelProvider(PackOutput output)
    {
        super(output, BeyondDimensions.MODID);
    }

    @Override
    protected void registerModels(@NotNull BlockModelGenerators blockModels, @NotNull ItemModelGenerators itemModels)
    {
        super.registerModels(blockModels, itemModels);

        blockWithItem(blockModels, BDBlocks.NET_CONTROL);
        blockWithItem(blockModels, BDBlocks.NET_INTERFACE);
        blockWithItem(blockModels, BDBlocks.NET_PATHWAY);
        blockWithItem(blockModels, BDBlocks.NET_ENERGY_PATHWAY);
        blockWithItem(blockModels, BDBlocks.DIMENSIONAL_CONNECT_BLOCK);

        blockModels.registerSimpleItemModel(BDBlocks.NET_TERMINAL_BLOCK.get(), Identifier.fromNamespaceAndPath(BeyondDimensions.MODID, "net_terminal_block"));
        blockModels.registerSimpleItemModel(BDBlocks.NET_PUMP_BLOCK.get(), Identifier.fromNamespaceAndPath(BeyondDimensions.MODID, "net_pump_block"));
        blockModels.registerSimpleItemModel(BDBlocks.NET_HOPPER_BLOCK.get(), Identifier.fromNamespaceAndPath(BeyondDimensions.MODID, "net_hopper_block"));
        blockModels.registerSimpleItemModel(BDBlocks.NET_FURNACE_BLOCK.get(), Identifier.fromNamespaceAndPath(BeyondDimensions.MODID, "net_furnace_block"));
    }

    private void blockWithItem(BlockModelGenerators blockModels, DeferredBlock<?> deferredBlock)
    {
        Block block = deferredBlock.get();
        blockModels.createTrivialCube(block);
        blockModels.registerSimpleItemModel(block, ModelLocationUtils.getModelLocation(block));
    }
}
