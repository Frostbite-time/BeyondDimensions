package com.wintercogs.beyonddimensions.Integration.RS.Block;

import com.wintercogs.beyonddimensions.Block.Custom.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class RSNetPathwayBlock extends NetedBlock implements EntityBlock
{
    public RSNetPathwayBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new RSNetPathwayBlockEntity(blockPos, blockState);
    }
}