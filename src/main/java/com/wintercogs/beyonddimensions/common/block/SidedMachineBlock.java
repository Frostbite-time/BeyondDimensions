package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.block.entity.BaseMachineBlockEntity;
import com.wintercogs.beyonddimensions.common.block.orientation.FacingRule;
import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 有面配置的机器方块：放置时正面朝向玩家，面配置按正面区分前后左右，见 {@link MachineSides}。
 * 能绕正面转的朝向用扳手（forge:tools/wrench）右键转，见 {@link FacingRule#turned}
 */
public abstract class SidedMachineBlock extends BaseMachineBlock
{
    private static final TagKey<Item> WRENCHES = ItemTags.create(new ResourceLocation("forge", "tools/wrench"));

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

    /**
     * 扳手先转动朝向；没有转动时交给 {@link #useWithoutWrench}
     */
    @Override
    public final @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        BlockState turned = player.getItemInHand(hand).is(WRENCHES) ? facingRule().turned(state) : null;
        if (turned == null)
            return useWithoutWrench(state, level, pos, player, hand, hitResult);
        if (!level.isClientSide())
        {
            level.setBlockAndUpdate(pos, turned);
            // 各面朝向变了，相邻方块要重新取能力
            if (level.getBlockEntity(pos) instanceof BaseMachineBlockEntity machine)
                machine.onFacingChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 没有用扳手转动朝向时的右键交互，子类重写它而不是 {@link #use}
     */
    protected InteractionResult useWithoutWrench(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        return super.use(state, level, pos, player, hand, hitResult);
    }
}
