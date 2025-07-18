package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// 网络熔炉
public class NetFurnaceBlock extends BaseMachineBlock
{

    public NetFurnaceBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new NetFurnaceBlockEntity(blockPos,blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        super.useWithoutItem(state,level,pos,player,hitResult);
        if(!level.isClientSide()&&!player.isShiftKeyDown())
        {
            player.openMenu((NetFurnaceBlockEntity)level.getBlockEntity(pos),pos);
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    // 处理掉落物
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof NetFurnaceBlockEntity blockEntity) {
                level.updateNeighbourForOutputSignal(pos, this);
                blockEntity.dropContent();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
