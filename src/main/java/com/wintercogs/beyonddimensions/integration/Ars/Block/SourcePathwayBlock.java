package com.wintercogs.beyonddimensions.integration.Ars.Block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SourcePathwayBlock extends NetedBlock implements EntityBlock
{
    public SourcePathwayBlock(Properties properties)
    {
        super(properties);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new SourcePathwayBlockEntity(blockPos, blockState);
    }
}