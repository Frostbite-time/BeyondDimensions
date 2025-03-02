package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NetEnergyPathwayBlock extends NetedBlock implements EntityBlock
{

    public NetEnergyPathwayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new NetEnergyPathwayBlockEntity(blockPos,blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        return super.useWithoutItem(state,level,pos,player,hitResult);
    }
}