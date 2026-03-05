package com.wintercogs.beyonddimensions.integration.Ars.Block;

import com.wintercogs.beyonddimensions.Block.Custom.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SourcePathwayBlock extends NetedBlock implements EntityBlock
{
    public SourcePathwayBlock(Properties properties)
    {
        super(properties);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new SourcePathwayBlockEntity(blockPos, blockState);
    }
}
