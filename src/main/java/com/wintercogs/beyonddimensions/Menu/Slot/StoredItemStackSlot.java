package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.DataBase.DimensionsItemStorage;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StoredItemStackSlot extends Slot
{
    // 一个空容器，仅用于欺骗父类构造函数，实际存储使用StoredItemStack并结合index
    private static final Container empty_inv = new SimpleContainer(0);
    private final DimensionsItemStorage itemStorage;
    private int theSlot;

    // 简介思路：构建一个slot，使用index结合DimensionsItemStorage中的列表来管理自身对应物品
    // 为此，需要重写网络沟通方案，将DimensionsItemStorage作为原inv，StoredItemStack作为原ItemStack来进行数据同步

    public StoredItemStackSlot(DimensionsItemStorage itemStorage, int slotIndex, int xPosition, int yPosition)
    {
        super(empty_inv, slotIndex, xPosition, yPosition);
        this.theSlot = slotIndex;
        this.itemStorage = itemStorage;
    }

    public ItemStack getVanillaActualStack()
    {
        //从当前槽索引取物品
        ItemStack itemStack = itemStorage.getItemStackByIndex(getSlotIndex());
        if (itemStack != null)
        {
            if(itemStack.getCount()>itemStack.getMaxStackSize())
            {
                ItemStack itemVanillaActualStack = itemStack.copy();
                itemVanillaActualStack.setCount(itemStack.getMaxStackSize());
                return itemVanillaActualStack;
            }
            else
            {
                return itemStack.copy();
            }

        }
        return ItemStack.EMPTY;
    }

    public ItemStack getVanillaMaxSizeItem()
    {
        //从当前槽索引取物品
        ItemStack itemStack = itemStorage.getItemStackByIndex(getSlotIndex());
        if (itemStack != null)
        {   //使用getActualStack将当前的真正总数返回，可以确保显示数量的正确
            ItemStack itemVanillaActualStack = itemStack.copy();
            itemVanillaActualStack.setCount(itemStack.getMaxStackSize());
            return itemVanillaActualStack;
        }
        return ItemStack.EMPTY;
    }


    // 以下这些重写 覆盖了slot中最基本的要素，以便将Container驱动的inv系统，替换成DimensionsItemStorage驱动
    @Override
    public ItemStack getItem()
    {
        if(getSlotIndex()<0)
        {
            return ItemStack.EMPTY;
        }
        //从当前槽索引取物品
        ItemStack itemStack = itemStorage.getItemStackByIndex(getSlotIndex());
        if (itemStack != null)
        {   //使用getActualStack将当前的真正总数返回，可以确保显示数量的正确
            return itemStack.copy();
        }
        return ItemStack.EMPTY;

    }

    @Override
    public boolean hasItem()
    {
        //检查当前槽是否为空
        return itemStorage.getItemStackByIndex(getSlotIndex()) != null
                && itemStorage.getItemStackByIndex(getSlotIndex()) != ItemStack.EMPTY;
    }

    @Override
    public void set(ItemStack stack)
    {
        if (stack == ItemStack.EMPTY || stack == null || getSlotIndex() <0)
            return;
        // 当尝试用一个物品真正覆盖这个槽内容会发生什么
        // 如果索引不存在，使用add自增长，如果存在，直接替换
        if (itemStorage.getItemStorage().size() > getSlotIndex())
            itemStorage.getItemStorage().set(getSlotIndex(), stack.copy());
        else if(itemStorage.getItemStorage().size() == getSlotIndex())
            itemStorage.getItemStorage().add(getSlotIndex(), stack.copy());
        else
        {
            //将size到Index-1之间的位置填充为空，然后填充Index位置
            // 扩展列表直到 targetIndex - 1，并填充 null
            while (itemStorage.getItemStorage().size() < getSlotIndex()) {
                itemStorage.getItemStorage().add(ItemStack.EMPTY);  // 填充空值
            }
            itemStorage.getItemStorage().add(getSlotIndex(), stack.copy());
        }


        this.setChanged();
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack)
    {
        // 当玩家拿着物品点击这个槽会发生什么
        itemStorage.addItem(newStack, newStack.getCount());
        this.setChanged();
    }


    @Override
    public void setChanged()
    {
        // 重要函数，确保存储被修改后net能被设定为脏数据保存
        this.itemStorage.OnChange();
    }

    @Override
    public int getMaxStackSize()
    {
        // 获取槽位可存储物品的最大值
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack)
    {
        // 获取槽位可存储物品的最大值
        return Integer.MAX_VALUE - 1;
    }

    @Override
    public ItemStack remove(int amount)
    {
        if (getItem() == ItemStack.EMPTY || getItem() == null)
        {
            return ItemStack.EMPTY;
        }
        // 从当前槽位移除对应数量的物品 并返回被移除的物品总数
        return itemStorage.removeItem(getSlotIndex(), amount);
    }

    @Override
    public int getSlotIndex()
    {
        return this.theSlot;
    }

    @Override
    public boolean isSameInventory(Slot other)
    {
        if (other instanceof StoredItemStackSlot)
        {
            // 比较二者是否是同一个引用  或许以后可以用其他更注重数据的方式比较？
            return this.itemStorage == ((StoredItemStackSlot) other).itemStorage;
        }
        return false;
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment)
    {
        // 如果传入的物品不是空气，也可以放入，则放入物品后返回剩余
        // 否则直接返回物品
        if (!stack.isEmpty() && this.mayPlace(stack))
        {
            ItemStack itemstack = this.getItem();
            int i = Math.min(Math.min(increment, stack.getCount()), this.getMaxStackSize(stack) - itemstack.getCount());
            this.setByPlayer(stack.split(i));
        }
        return stack;
    }

    @Override
    public int getContainerSlot()
    {
        return this.theSlot;
    }

    public void setTheSlotIndex(int index)
    {
        this.theSlot = index;
    }

    public int getItemCount()
    {
        if(getSlotIndex()<0)
        {
            return -1;
        }
        //从当前槽索引取物品
        ItemStack itemStack = itemStorage.getItemStackByIndex(getSlotIndex());
        if (itemStack != null && itemStack != ItemStack.EMPTY)
        {   //使用getActualStack将当前的真正总数返回，可以确保显示数量的正确
            return itemStack.getCount();
        }
        return -1;
    }

}
