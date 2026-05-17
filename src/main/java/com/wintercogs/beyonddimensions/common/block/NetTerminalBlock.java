package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.block.entity.NetTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NetTerminalBlock extends NetedBlock implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // 定义6个方向的碰撞箱（单位：1/16方块）
    private static final VoxelShape COLLISION_NORTH = Block.box(0, 0, 0, 16, 16, 3);  // 北向：厚度4的板
    private static final VoxelShape COLLISION_SOUTH = Block.box(0, 0, 13, 16, 16, 16); // 南向
    private static final VoxelShape COLLISION_EAST = Block.box(13, 0, 0, 16, 16, 16);  // 东向
    private static final VoxelShape COLLISION_WEST = Block.box(0, 0, 0, 3, 16, 16);    // 西向
    private static final VoxelShape COLLISION_UP = Block.box(0, 13, 0, 16, 16, 16);     // 上向
    private static final VoxelShape COLLISION_DOWN = Block.box(0, 0, 0, 16, 3, 16);    // 下向

    public NetTerminalBlock(Properties properties)
    {
        super(properties
                .noOcclusion()
                .dynamicShape());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    // 注册方块状态属性
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot)
    {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return getCollisionShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        Direction facing = state.getValue(FACING);
        return switch (facing)
        {
            case NORTH -> COLLISION_NORTH;
            case SOUTH -> COLLISION_SOUTH;
            case EAST -> COLLISION_EAST;
            case WEST -> COLLISION_WEST;
            case UP -> COLLISION_UP;
            case DOWN -> COLLISION_DOWN;
        };
    }

    // 添加放置时自动设置朝向的逻辑
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context)
    {
        // 获取玩家看向的方向的反方向作为方块朝向
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        super.useWithoutItem(state, level, pos, player, hitResult);
        if (!level.isClientSide() && !player.isShiftKeyDown())
        {
            NetTerminalBlockEntity blockEntity = (NetTerminalBlockEntity) level.getBlockEntity(pos);
            if (blockEntity.getNet() != null)
                player.openMenu((NetTerminalBlockEntity) level.getBlockEntity(pos));
            else
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_need_bound"));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new NetTerminalBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock()))
        {
            if (level.getBlockEntity(pos) instanceof NetTerminalBlockEntity blockEntity)
            {
                level.updateNeighbourForOutputSignal(pos, this);
                blockEntity.dropContent();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
