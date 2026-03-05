package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.Interface.IAddNetMemberHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetManagerInviter extends NetedItem implements IAddNetMemberHandler
{
    public NetManagerInviter(Properties properties)
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
    protected boolean validToReWrite(DimensionsNet net, Player player)
    {
        return net.isOwner(player);
    }

    @Override
    public boolean AddPlayerToNet(DimensionsNet net, Player player)
    {
        if (net != null)
        {
            net.addManager(player.getUUID());
            return true;
        }

        return false;
    }
}
