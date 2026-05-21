package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class NetFurnaceBlockEntity extends BaseNetFurnaceBlockEntity<SmeltingRecipe>
{
    public NetFurnaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_FURNACE_BLOCK_ENTITY.get(), pos, blockState, RecipeType.SMELTING, Component.translatable("menu.title.beyonddimensions.furnace_menu"));
    }

    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        BaseNetFurnaceBlockEntity.registerItemHandlerCapability(event, BDBlockEntities.NET_FURNACE_BLOCK_ENTITY);
        BaseNetFurnaceBlockEntity.registerItemHandlerCapability(event, BDBlockEntities.NET_BLAST_FURNACE_BLOCK_ENTITY);
        BaseNetFurnaceBlockEntity.registerItemHandlerCapability(event, BDBlockEntities.NET_SMOKER_BLOCK_ENTITY);
    }
}
