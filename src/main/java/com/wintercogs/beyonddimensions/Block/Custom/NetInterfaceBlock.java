package com.wintercogs.beyonddimensions.Block.Custom;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.Block.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.Block.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsItemStorage;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class NetInterfaceBlock extends Block implements EntityBlock
{

    private static final Logger LOGGER = LogUtils.getLogger();

    public NetInterfaceBlock(Properties properties)
    {
        super(properties);
    }

    // 启用方块实体计时器
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
                (level1, pos, state1, blockEntity) ->
                        NetInterfaceBlockEntity.tick(level1, pos, state1, (NetInterfaceBlockEntity) blockEntity);
    }

//    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
//        super.tick(state,level,pos,random);
//        LOGGER.info("tick执行");
//        if(level.getBlockEntity(pos) instanceof NetInterfaceBlockEntity blockEntity)
//        {
//            if(blockEntity.getNetId() != -1)
//            {
//                blockEntity.transTime++;
//                if(blockEntity.transTime>=blockEntity.transHold)
//                {
//                    blockEntity.transTime = 0;
//                    blockEntity.transferToNet(level);
//                }
//            }
//        }
//    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        // 空手右键可以设定网络接口所绑定的网络
        if(!player.getMainHandItem().isEmpty())
        {
            return InteractionResult.PASS;
        }
        if(!level.isClientSide())
        {
            if(level.getBlockEntity(pos) instanceof NetInterfaceBlockEntity blockEntity)
            {
                if(blockEntity.getNetId() == -1)
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net != null)
                    {
                        blockEntity.setNetId(net.getId());
                    }
                }
                else
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net != null)
                    {
                        if(net.getId() == blockEntity.getNetId())
                        {
                            if(net.isManager(player))
                            {
                                blockEntity.setNetId(-1);
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity)
    {
        if(!level.isClientSide())
        {
            if(level.getBlockEntity(pos) instanceof NetInterfaceBlockEntity blockEntity)
            {
                LOGGER.info("目标方块的网络id为:{}",blockEntity.getNetId());
            }
        }
        super.stepOn(level,pos,state,entity);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof NetInterfaceBlockEntity blockEntity) {
                Containers.dropContents(level, pos, blockEntity);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new NetInterfaceBlockEntity(blockPos,blockState);
    }
}
