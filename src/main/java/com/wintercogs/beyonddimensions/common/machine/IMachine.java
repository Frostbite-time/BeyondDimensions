package com.wintercogs.beyonddimensions.common.machine;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IMachine
{
    // 用于方块
    public void working();

    // 用于物品
    public void working(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected);

    // 用于方块
    public boolean shouldWork();

    // 用于物品
    public boolean shouldWork(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected);

}
