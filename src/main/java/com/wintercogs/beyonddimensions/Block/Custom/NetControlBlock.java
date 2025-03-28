package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
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
        super.useWithoutItem(state,level,pos,player,hitResult);
        if(!level.isClientSide())
        {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, _player) -> new NetControlMenu(containerId,playerInventory),
                    Component.translatable("menu.title.beyonddimensions.net_control_menu")
            ));
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }

}
