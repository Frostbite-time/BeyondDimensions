package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    // 启用方块实体计时器
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
                (level1, pos, state1, blockEntity) ->
                        NetEnergyPathwayBlockEntity.tick(level1, pos, state1, (NetEnergyPathwayBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        super.useWithoutItem(state,level,pos,player,hitResult);
        if(!level.isClientSide()&&!player.isShiftKeyDown())
        {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, _player) -> new NetEnergyMenu(containerId, _player.getInventory(), ((NetEnergyPathwayBlockEntity) level.getBlockEntity(pos)),new SimpleContainerData(0)),
                    Component.translatable("menu.title.beyonddimensions.net_energy_menu")
            ));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }
}