package com.wintercogs.beyonddimensions.Util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;

public class ItemStackHelper
{
    // 此物品堆的组件被修改过
    public static boolean hasExtraComponents(ItemStack stack)
    {
        DataComponentMap comps = stack.getComponents();

        // 检查补丁是否为空
        if (comps instanceof PatchedDataComponentMap patched)
        {
            return !patched.isPatchEmpty();   // 组件被修改
        }

        // 无补丁
        return false;
    }
}
