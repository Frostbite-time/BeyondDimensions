package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class UnstableSpaceTimeFragment extends Item
{
    public UnstableSpaceTimeFragment(Properties properties)
    {
        super(properties.component(ModDataComponents.LONG_DATA,3600L*20L));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if(!level.isClientSide() &&entity instanceof Player player)
        {
            if(stack.get(ModDataComponents.LONG_DATA) >0)
            {
                stack.set(ModDataComponents.LONG_DATA,stack.get(ModDataComponents.LONG_DATA)-1);
            }
            else
            {
                ItemStack stable = new ItemStack(ModItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
                player.getInventory().setItem(slotId, stable);
            }
        }


    }
}
