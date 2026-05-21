package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.block.entity.NetSmokerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetSmokerBlock extends BaseNetFurnaceBlock
{
    public NetSmokerBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState)
    {
        return new NetSmokerBlockEntity(blockPos, blockState);
    }
}
