package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class NetDestroyer extends NetedItem
{
    public NetDestroyer(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return 100; // 5秒
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        // 仅限主手且非潜行状态
        if (usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResultHolder.fail(itemstack);
        }
        // 启动使用过程
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity)
    {
        super.finishUsingItem(stack, level, livingEntity);

        if (!(livingEntity instanceof Player player)) return stack;

        if (!level.isClientSide())
        {
            if (NetedItem.getNetId(stack) >= 0)
            {
                DimensionsNet itemNet = DimensionsNet.getNetFromId(NetedItem.getNetId(stack));
                if (itemNet != null)
                {
                    DimensionsNet playerNet = DimensionsNet.getNetFromPlayer(player);
                    // 只有网络主人能删除自己的网络
                    if (playerNet != null && playerNet.getId() == itemNet.getId() && playerNet.isOwner(player))
                    {
                        playerNet.destroySelf();
                        stack.shrink(1);
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_net_destroyed"));
                    }
                    else
                    {
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.cant_delete_net"));
                    }
                }
                else
                {
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.error_item_net"));
                }
            }
            else
            {
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_need_bound"));
            }
        }
        return stack;
    }

    @Override
    protected boolean validToReWrite(DimensionsNet net, Player player)
    {
        return net.isOwner(player);
    }
}
