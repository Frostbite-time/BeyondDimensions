package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

// 以IStackType为基础实现IItemHandler的类
public class ItemUnifiedStorageHandler implements IItemHandler
{
    private UnifiedStorage storage;

    public ItemUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }


    @Override
    public int getSlots()
    {
        List<Integer> slots = storage.getTypeIdIndexList(ItemStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = storage.getTypeIdIndexList(ItemStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (ItemStack)storage.getStackBySlot(actualIndex).getStack();
        }
        else return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        ItemStackType typedStack = (ItemStackType) storage.insert(new ItemStackType(itemStack),sim);
        return typedStack.getStack();
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        // 这个调用链分为三步
        // 1.专为物品提供的假列表中获取指定物品并转为IStackType
        // 2.使用存储器导出
        // 3.获取返回值的Stack，然后转为ItemStack再返回
        return (ItemStack) storage.extract(new ItemStackType(getStackInSlot(slot).copyWithCount(count)),sim)
                .getStack();
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return Integer.MAX_VALUE-1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack)
    {
        return true;
    }
}
