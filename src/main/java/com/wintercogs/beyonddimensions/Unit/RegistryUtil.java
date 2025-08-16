package com.wintercogs.beyonddimensions.Unit;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class RegistryUtil
{
    /** 安全获取某个 Item 的 Holder；未知/为空则回退 AIR */
    public static Holder<Item> holderOf(Item item) {
        var reg = BuiltInRegistries.ITEM;

        if (item == null) {
            return reg.getHolderOrThrow(reg.getResourceKey(Items.AIR).orElseThrow());
        }

        // 已注册就用它的 key，否则回退 AIR
        return reg.getResourceKey(item)
                .map(reg::getHolderOrThrow)
                .orElseGet(() -> reg.getHolderOrThrow(reg.getResourceKey(Items.AIR).orElseThrow()));
    }
}
