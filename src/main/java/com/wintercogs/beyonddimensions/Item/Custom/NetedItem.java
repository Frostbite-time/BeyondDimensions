package com.wintercogs.beyonddimensions.Item.Custom;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class NetedItem extends Item
{
    public NetedItem(Properties properties) {
        super(properties.component(ModDataComponents.NET_ID_DATA, -1));
    }
    public static final Logger LOGGER = LogUtils.getLogger();


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND)
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if(!level.isClientSide())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null)
            {
                if(validToReWrite(net,player))
                {
                    if(itemstack.get(ModDataComponents.NET_ID_DATA) != net.getId())
                    {
                        itemstack.set(ModDataComponents.NET_ID_DATA,net.getId());
                    }
                    else
                    {
                        itemstack.set(ModDataComponents.NET_ID_DATA,-1);
                    }
                }
                else
                {
                    return InteractionResultHolder.fail(itemstack);
                }
            }
            else
            {
                return InteractionResultHolder.fail(itemstack);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

    // 覆写此方法以实现自定义网络覆写规则
    protected boolean validToReWrite(DimensionsNet net, Player player )
    {
        return net.isManager(player);
    }
}
