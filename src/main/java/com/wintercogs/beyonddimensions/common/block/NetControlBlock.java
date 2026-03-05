package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetControlBlock extends Block
{
    public NetControlBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        super.useWithoutItem(state, level, pos, player, hitResult);
        if (!level.isClientSide())
        {
            if (DimensionsNet.getNetFromPlayer(player) != null)
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new NetControlMenu(containerId, playerInventory),
                        Component.translatable("menu.title.beyonddimensions.net_control_menu")
                ));
            }
        }
        return InteractionResult.SUCCESS;
    }

}
