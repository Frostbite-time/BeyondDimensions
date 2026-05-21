package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class NetBlastFurnaceBlockEntity extends BaseNetFurnaceBlockEntity<BlastingRecipe>
{
    public NetBlastFurnaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_BLAST_FURNACE_BLOCK_ENTITY.get(), pos, blockState, RecipeType.BLASTING, Component.translatable("menu.title.beyonddimensions.blast_furnace_menu"));
    }
}
