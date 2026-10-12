package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.state.BlockState;

public class NetSmokerBlockEntity extends BaseNetFurnaceBlockEntity<SmokingRecipe>
{
    public NetSmokerBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_SMOKER_BLOCK_ENTITY.get(), pos, blockState, RecipeType.SMOKING, Component.translatable("menu.title.beyonddimensions.smoker_menu"));
    }

    @Override
    public MachineType machineType()
    {
        return BDMachineTypes.NET_SMOKER.get();
    }
}
