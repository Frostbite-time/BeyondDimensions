package com.wintercogs.beyonddimensions.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;

public class ItemStackHelper
{
    /**
     * 检查对应物品堆的组件是否被修改过
     * <p>等价于1.20.1中检查物品堆是否带额外NBT数据</p>
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
