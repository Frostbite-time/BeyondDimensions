package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

/**
 * 有面配置的机器方块：放置时正面朝向玩家，面配置按正面区分前后左右，见 {@link MachineSides}。
 * 能绕正面转的朝向用扳手（c:tools/wrench）右键转，见 {@link FacingRule#turned}
 */
public abstract class SidedMachineBlock extends BaseMachineBlock
{
    public SidedMachineBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(facingRule().defaultState(stateDefinition.any()));
    }

    /**
     * 正面能朝哪些方向，见 {@link FacingRule}。方块构造时就会调用，只能返回常量
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

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        BlockState turned = stack.is(Tags.Items.TOOLS_WRENCH) ? facingRule().turned(state) : null;
        if (turned == null)
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        if (!level.isClientSide())
        {
            level.setBlockAndUpdate(pos, turned);
            // 各面朝向变了，相邻方块要重新取能力
            if (level.getBlockEntity(pos) instanceof BlockEntity be)
                be.invalidateCapabilities();
            else
                level.invalidateCapabilities(pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
