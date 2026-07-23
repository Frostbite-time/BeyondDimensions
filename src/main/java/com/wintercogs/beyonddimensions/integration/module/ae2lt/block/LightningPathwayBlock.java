package com.wintercogs.beyonddimensions.integration.module.ae2lt.block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.block.entity.LightningPathwayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LightningPathwayBlock extends NetedBlock implements EntityBlock
{
    public LightningPathwayBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new LightningPathwayBlockEntity(pos, state);
    }
}
