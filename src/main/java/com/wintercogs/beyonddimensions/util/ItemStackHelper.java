package com.wintercogs.beyonddimensions.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;

public class ItemStackHelper
{
    /**
     * 检查此堆叠的组件是否被修改过，即是否有除了默认组件之外的数据
     */
    public static boolean hasExtraComponents(ItemStack stack)
    {
        DataComponentMap comps = stack.getComponents();

        if (comps instanceof PatchedDataComponentMap patched)
        {
            return !patched.isPatchEmpty();
        }
        return false;
    }
}
