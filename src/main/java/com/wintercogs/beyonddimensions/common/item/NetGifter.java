package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetGifter extends NetedItem
{

    public NetGifter(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if (!level.isClientSide())
        {

            if (NetedItem.getNetId(itemstack) >= 0)
            {
                DimensionsNet itemNet = DimensionsNet.getNetFromId(NetedItem.getNetId(itemstack));
                if (itemNet != null)
                {
                    DimensionsNet playerNet = DimensionsNet.getNetFromPlayer(player);
                    // 只有网络主人能合并其他人的网络
                    if (playerNet != null && playerNet.getId() != itemNet.getId() && playerNet.isOwner(player))
                    {
                        int id = itemNet.getId();
                        playerNet.mergeOtherNet(itemNet);
                        itemstack.shrink(1);
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.net_gift_done", id));
                    }
                    else
                        player.sendSystemMessage(Component.translatable("msg.beyonddimensions.cant_merge_net"));
                }
                else
                    player.sendSystemMessage(Component.translatable("msg.beyonddimensions.error_item_net"));
            }
            else
            {
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_need_bound"));
            }

        }
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    protected boolean validToReWrite(DimensionsNet net, Player player)
    {
        return net.isOwner(player);
    }
}
