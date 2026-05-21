package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.block.entity.NetBlastFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetBlastFurnaceBlock extends BaseNetFurnaceBlock
{
    public NetBlastFurnaceBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState)
    {
        return new NetBlastFurnaceBlockEntity(blockPos, blockState);
    }
}
