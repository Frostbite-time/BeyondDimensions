package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * 有面配置的机器方块：放置时正面朝向玩家，面配置按正面区分前后左右，见 {@link MachineSides}
 */
public abstract class SidedMachineBlock extends BaseMachineBlock
{
    public SidedMachineBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(facingRule().defaultState(stateDefinition.any()));
    }

    /**
     * 正面能朝哪些方向；换成 {@link FacingRule#ALL} 后正面也能朝上朝下。方块构造时就会调用，只能返回常量
     */
    protected FacingRule facingRule()
    {
        return FacingRule.HORIZONTAL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        facingRule().addProperties(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return facingRule().placed(defaultBlockState(), context);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation)
    {
        return facingRule().rotate(state, rotation);
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror)
    {
        return facingRule().mirror(state, mirror);
    }
}
