package com.wintercogs.beyonddimensions.DataBase;


import net.minecraft.world.item.ItemStack;

// 作为一个可以存储无限数量的 ItemStack
public class StoredItemStack
{
    private ItemStack itemStack;
    private long count;

    public StoredItemStack(ItemStack itemStack)
    {
        this.itemStack = itemStack.copy();
        this.itemStack.setCount(1);
        this.count = 1;
    }

    public StoredItemStack(ItemStack itemStack, long count)
    {
        this.itemStack = itemStack.copy();
        this.itemStack.setCount(1);
        this.count = count;
    }

    public ItemStack getItemStack()
    {
        return itemStack;
    }

    public long getCount()
    {
        return count;
    }

    public void addCount(long num)
    {
        this.count += num;
    }

    public void subCount(long num)
    {
        this.count -= num;
        if (this.count <= 0)
        {
            this.count = 0;
        }
    }


    public ItemStack getActualStack()
    {
        ItemStack s = itemStack.copy();
        s.setCount((int) count);
        return s;
    }

    public ItemStack getVanillaMaxSizeStack()
    {
        ItemStack s = itemStack.copy();
        s.setCount(s.getMaxStackSize());
        return s;
    }


    // 用于比较2个StoredItemStack储存的是否是完全一致的物品  不检查数量
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        StoredItemStack other = (StoredItemStack) obj;
        if (itemStack == null)
        {
            if (other.itemStack != null) return false;
        }
        else if (!ItemStack.isSameItemSameComponents(itemStack, other.itemStack)) return false;
        return true;
    }

    // 不检查数量的比较
    public boolean equals(StoredItemStack other)
    {
        if (this == other) return true;
        if (other == null) return false;
        if (itemStack == null)
        {
            if (other.itemStack != null) return false;
        }
        else if (!ItemStack.isSameItemSameComponents(itemStack, other.itemStack)) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        return ItemStack.hashItemAndComponents(this.itemStack);
    }
}
