package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.Block.BlockEntity.Custom.NetFluidPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Block.BlockEntity.Custom.NetPathwayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NetFluidPathwayBlock extends NetedBlock implements EntityBlock
{

    public NetFluidPathwayBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new NetFluidPathwayBlockEntity(blockPos,blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        return super.useWithoutItem(state,level,pos,player,hitResult);
    }
}
