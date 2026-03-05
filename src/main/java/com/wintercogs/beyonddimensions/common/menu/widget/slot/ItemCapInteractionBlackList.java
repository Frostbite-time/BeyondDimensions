package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.Set;

// 手持对应物品交互，即使有相关能力也不触发操作
public class ItemCapInteractionBlackList
{
    private static Set<Item> blacklist = new HashSet<Item>();

    public static boolean addToBlackList(Item item)
    {
        return blacklist.add(item);
    }

    public static boolean removeFromBlackList(Item item)
    {
        return blacklist.remove(item);
    }

    public static boolean isInBlackList(Item item)
    {
        return blacklist.contains(item);
    }

}
