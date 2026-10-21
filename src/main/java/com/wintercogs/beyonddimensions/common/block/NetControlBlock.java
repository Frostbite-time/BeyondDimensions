package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.menu.NetControlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class NetControlBlock extends Block
{
    public NetControlBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        super.use(state, level, pos, player, hand, hitResult);
        if (!level.isClientSide())
        {
            if (DimensionsNet.getPrimaryNetFromPlayer(player) != null)
            {
                NetworkHooks.openScreen((ServerPlayer) player, new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new NetControlMenu(containerId, playerInventory),
                        Component.translatable("menu.title.beyonddimensions.net_control_menu")
                ));
            }
        }
        return InteractionResult.SUCCESS;
    }

}
