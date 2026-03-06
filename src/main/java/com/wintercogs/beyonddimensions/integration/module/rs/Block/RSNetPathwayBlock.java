package com.wintercogs.beyonddimensions.integration.module.rs.Block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RSNetPathwayBlock extends NetedBlock implements EntityBlock
{
    public RSNetPathwayBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new RSNetPathwayBlockEntity(blockPos, blockState);
    }
}
