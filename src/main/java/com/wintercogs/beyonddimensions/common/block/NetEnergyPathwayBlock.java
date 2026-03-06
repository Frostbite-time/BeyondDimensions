package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NetEnergyPathwayBlock extends BaseMachineBlock
{

    public NetEnergyPathwayBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState)
    {
        return new NetEnergyPathwayBlockEntity(blockPos, blockState);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        super.use(state, level, pos, player, hand, hitResult);
        if (!level.isClientSide() && !player.isShiftKeyDown())
        {
            NetworkHooks.openScreen((ServerPlayer) player, (NetEnergyPathwayBlockEntity) level.getBlockEntity(pos), pos);
        }
        return InteractionResult.SUCCESS;
    }
}