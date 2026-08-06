package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NetedBlock extends Block
{

    public NetedBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        if (placer instanceof Player player)
        {
            if (!level.isClientSide())
            {
                if (level.getBlockEntity(pos) instanceof NetedBlockEntity blockEntity)
                {
                    if (blockEntity.getNetId() == -1)
                    {
                        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                        if (net != null)
                        {
                            if (net.isManager(player))
                            {
                                // 成功设置网络id
                                blockEntity.setNetId(net.getId());
                                level.invalidateCapabilities(pos); // 用于清除实体能力缓存
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_bound", net.getId()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        if (!player.getMainHandItem().isEmpty() || !player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide())
        {
            if (level.getBlockEntity(pos) instanceof NetedBlockEntity blockEntity)
            {
                if (blockEntity.getNetId() < 0)
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if (net != null)
                    {
                        if (net.isManager(player))
                        {
                            blockEntity.setNetId(net.getId());
                            level.invalidateCapabilities(pos);
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_bound", net.getId()));
                        }
                        else
                        {
                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_block"));
                        }
                    }
                }
                else
                {
                    int currentNetId = blockEntity.getNetId();
                    DimensionsNet currentNet = DimensionsNet.getNetFromId(currentNetId);
                    if (currentNet == null)
                    {
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_unbound", currentNetId));
                        blockEntity.setNetId(-1);
                        level.invalidateCapabilities(pos);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
                    }
                    else if (currentNet.isManager(player))
                    {
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_unbound", currentNetId));
                        blockEntity.setNetId(-1);
                        level.invalidateCapabilities(pos);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
                    }
                    else
                    {
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_block"));
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston)
    {
        if (level.getBlockEntity(pos) instanceof NetedBlockEntity blockEntity)
        {
            blockEntity.clearNetId();
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
