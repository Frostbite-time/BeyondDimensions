package com.wintercogs.beyonddimensions.Block.Custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NetInterfaceBlock extends Block
{
    public NetInterfaceBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity)
    {
        if(entity instanceof ItemEntity itemEntity)
        {
            if(itemEntity.getItem().getItem() != Items.DIAMOND)
                itemEntity.setItem(new ItemStack(Items.DIAMOND,itemEntity.getItem().getCount()));
        }

        super.stepOn(level,pos,state,entity);
    }
}
