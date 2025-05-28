package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    public NetedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        if(placer instanceof Player player)
        {
            if(!level.isClientSide())
            {
                if(level.getBlockEntity(pos) instanceof NetedBlockEntity blockEntity)
                {
                    if(blockEntity.getNetId() == -1)
                    {
                        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                        if(net != null)
                        {
                            if(net.isManager(player))
                            {
                                // 成功设置网络id
                                blockEntity.setNetId(net.getId());
                                level.invalidateCapabilities(pos); // 用于清除实体能力缓存
                                // 我觉得主动方式无需再弹音效
                                //level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,0.5F,1.0F);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_bound",net.getId()));
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
        // 空手右键可以设定网络接口所绑定的网络
        if(!player.getMainHandItem().isEmpty()||!player.isShiftKeyDown())
        {
            return InteractionResult.PASS;
        }
        if(!level.isClientSide())
        {
            if(level.getBlockEntity(pos) instanceof NetedBlockEntity blockEntity)
            {
                if(blockEntity.getNetId() == -1)
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net != null)
                    {
                        if(net.isManager(player))
                        {
                            // 成功设置网络id
                            blockEntity.setNetId(net.getId());
                            level.invalidateCapabilities(pos); // 用于清除实体能力缓存
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,0.5F,1.0F);
                            player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_bound",net.getId()));
                        }
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
                                // 成功清除网络id
                                blockEntity.setNetId(-1);
                                level.invalidateCapabilities(pos); // 用于清除实体能力缓存
                                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS,0.5F,1.0F);
                                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.block_net_unbound",net.getId()));
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }


}
