package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NetPathwayBlock extends NetedBlock implements EntityBlock {

    public NetPathwayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new NetPathwayBlockEntity(blockPos,blockState);
    }

}
