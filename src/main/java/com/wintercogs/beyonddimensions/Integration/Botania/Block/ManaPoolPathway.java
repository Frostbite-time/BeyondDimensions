package com.wintercogs.beyonddimensions.Integration.Botania.Block;

import com.wintercogs.beyonddimensions.Block.Custom.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import vazkii.botania.common.item.BotaniaItems;

import javax.annotation.Nullable;

public class ManaPoolPathway extends NetedBlock implements EntityBlock
{
    private static final VoxelShape SHAPE;
    private static final VoxelShape SHAPE_INTERACT;
    static {
        SHAPE_INTERACT = box(0, 0, 0, 16, 8, 16);
        VoxelShape cutout = box(2, 2, 2, 14, 16, 14);
        SHAPE = Shapes.join(SHAPE_INTERACT, cutout, BooleanOp.ONLY_FIRST);
    }


    public ManaPoolPathway(Properties properties)
    {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }


    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return (level1, blockPos, blockState, blockEntity) -> {
            if(blockEntity instanceof ManaPoolPathwayBlockEntity pool)
            {
                if(level1.isClientSide())
                    ManaPoolPathwayBlockEntity.clientTick(level1,blockPos,blockState,pool);
                else if(!level1.isClientSide())
                    ManaPoolPathwayBlockEntity.serverTick(level1,blockPos,blockState,pool);
            }
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new ManaPoolPathwayBlockEntity(blockPos, blockState);
    }
}
