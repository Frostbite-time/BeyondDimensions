package com.wintercogs.beyonddimensions.Block.Custom;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class NetInterfaceBlock extends NetedBlock implements EntityBlock
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        super.useWithoutItem(state,level,pos,player,hitResult);
        if(!level.isClientSide()&&!player.isShiftKeyDown())
        {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, _player) -> new NetInterfaceBaseMenu(containerId,_player.getInventory(),((NetInterfaceBlockEntity)level.getBlockEntity(pos)).getStackHandler() ,((NetInterfaceBlockEntity)level.getBlockEntity(pos)).getFakeStackHandler(),((NetInterfaceBlockEntity)level.getBlockEntity(pos)),new SimpleContainerData(0)),
                    Component.translatable("menu.title.beyonddimensions.net_interface_menu")
            ));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof NetInterfaceBlockEntity blockEntity) {
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor)
    {
        super.onNeighborChange(state, level, pos, neighbor);
        if (level.getBlockEntity(pos) instanceof NetInterfaceBlockEntity blockEntity) {
            blockEntity.setNeedsCapabilityUpdate();
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new NetInterfaceBlockEntity(blockPos,blockState);
    }
}
