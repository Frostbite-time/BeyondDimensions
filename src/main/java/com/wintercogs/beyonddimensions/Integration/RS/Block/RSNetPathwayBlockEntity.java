package com.wintercogs.beyonddimensions.Integration.RS.Block;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class RSNetPathwayBlockEntity extends NetedBlockEntity
{

    public RSNetPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.RS_NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

}
