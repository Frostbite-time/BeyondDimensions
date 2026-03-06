package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

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
                                // 我觉得主动方式无需再弹音效
                                //level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,0.5F,1.0F);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_bound", net.getId()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        // 空手右键可以设定网络接口所绑定的网络
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
                            // 成功设置网络id
                            blockEntity.setNetId(net.getId());
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_bound", net.getId()));
                        }
                        else
                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_block"));

                    }
                }
                else
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if (net != null)
                    {
                        if (net.getId() == blockEntity.getNetId() || DimensionsNet.getNetFromId(blockEntity.getNetId()) == null)
                        {
                            if (net.isManager(player))
                            {
                                // 成功清除网络id
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_unbound", blockEntity.getNetId()));
                                blockEntity.setNetId(-1);
                                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
                            }
                            else
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.no_right_to_bound_block"));
                        }
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

}
