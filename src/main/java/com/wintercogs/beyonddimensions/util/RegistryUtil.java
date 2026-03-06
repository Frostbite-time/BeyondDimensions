package com.wintercogs.beyonddimensions.util;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

public class RegistryUtil
{
    // 把弃用封装在包装中，方便后续修改
    public static Holder<Item> holderOf(Item item)
    {
        return item.builtInRegistryHolder();
    }

    public static Holder<Fluid> holderOf(Fluid fluid)
    {
        return fluid.builtInRegistryHolder();
    }
}
