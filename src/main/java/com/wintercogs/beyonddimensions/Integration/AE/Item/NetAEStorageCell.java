package com.wintercogs.beyonddimensions.Integration.AE.Item;

import com.wintercogs.beyonddimensions.Item.Custom.NetedItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetAEStorageCell extends NetedItem
{
    public NetAEStorageCell(Properties properties)
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

        return InteractionResultHolder.pass(itemstack);
    }
}
