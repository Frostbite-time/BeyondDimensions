package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Item.Interface.IAddNetMemberHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetManagerInviter  extends NetedItem implements IAddNetMemberHandler {
    public NetManagerInviter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND)
        {
            return InteractionResultHolder.fail(itemstack);
        }
        if(!level.isClientSide())
        {
            if(DimensionsNet.getNetFromPlayer(player) == null)
            {
                if(NetedItem.getNetId(itemstack)>=0)
                {
                    boolean flag = AddPlayerToNet(DimensionsNet.getNetFromId(NetedItem.getNetId(itemstack),level),player);
                    if (flag)
                    {
                        itemstack.shrink(1);
                    }
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

    @Override
    protected boolean validToReWrite(DimensionsNet net, Player player) {
        return net.isOwner(player);
    }

    @Override
    public boolean AddPlayerToNet(DimensionsNet net, Player player)
    {
        if(net != null)
        {
            net.addManager(player.getUUID());
            return true;
        }

        return false;
    }
}
