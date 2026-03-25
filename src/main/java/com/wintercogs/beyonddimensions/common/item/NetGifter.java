package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// 网络赠送符
public class NetGifter extends NetedItem
{

    public NetGifter(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide())
        {

            if (itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1) >= 0)
            {
                DimensionsNet itemNet = DimensionsNet.getNetFromId(itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1));
                if (itemNet != null)
                {
                    DimensionsNet playerNet = DimensionsNet.getNetFromPlayer(player);
                    // 只有网络主人能合并其他人的网络
                    if (playerNet != null && playerNet.getId() != itemNet.getId() && playerNet.isOwner(player))
                    {
                        int id = itemNet.getId();
                        playerNet.mergeOtherNet(itemNet);
                        itemstack.consume(1, player);
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
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean validToReWrite(DimensionsNet net, Player player)
    {
        return net.isOwner(player);
    }
}
