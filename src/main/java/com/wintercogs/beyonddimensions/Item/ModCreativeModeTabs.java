package com.wintercogs.beyonddimensions.Item;

import com.wintercogs.beyonddimensions.Block.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;


public class ModCreativeModeTabs
{

    // 物品分类标签
    public static final CreativeTabs ITEMS_TAB = new CreativeTabs("beyonddimensions_items")
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(ModItems.NET_CREATER);
        }

    };

    // 方块分类标签
    public static final CreativeTabs BLOCKS_TAB = new CreativeTabs("beyonddimensions_blocks")
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(ModBlocks.NET_CONTROL);
        }

    };
}

