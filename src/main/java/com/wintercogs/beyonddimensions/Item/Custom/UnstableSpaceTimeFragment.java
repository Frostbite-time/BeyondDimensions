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
        super(properties.component(ModDataComponents.LONG_DATA,3600L).component(ModDataComponents.TIME_LINE,0L));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if(!level.isClientSide() && entity instanceof Player player)
        {
            // 每隔10秒更新一次，频繁更新属性会导致频繁读写和网络同步
            final long currentTick = level.getGameTime();
            final long lastProcessed = stack.get(ModDataComponents.TIME_LINE);
            if(currentTick - lastProcessed > 200L)
            {
                if(stack.get(ModDataComponents.LONG_DATA) >0)
                {
                    stack.set(ModDataComponents.LONG_DATA,stack.get(ModDataComponents.LONG_DATA)-10);
                }
                else
                {
                    ItemStack stable = new ItemStack(ModItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
                    player.getInventory().setItem(slotId, stable);
                }
                stack.set(ModDataComponents.TIME_LINE, currentTick);
            }
        }
    }
}
