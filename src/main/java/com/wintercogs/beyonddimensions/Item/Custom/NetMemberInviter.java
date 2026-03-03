package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.Item.Interface.IAddNetMemberHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetMemberInviter extends NetedItem implements IAddNetMemberHandler
{
    public NetMemberInviter(Properties properties)
    {
        super(properties);
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
            if (DimensionsNet.getNetFromPlayer(player) == null)
            {
                if (itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1) >= 0)
                {
                    boolean flag = AddPlayerToNet(DimensionsNet.getNetFromId(itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1)), player);
                    if (flag)
                    {
                        itemstack.consume(1, player);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean AddPlayerToNet(DimensionsNet net, Player player)
    {
        if (net != null)
        {
            net.addPlayer(player.getUUID());
            return true;
        }

        return false;
    }

}
