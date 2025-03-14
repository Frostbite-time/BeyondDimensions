package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

// 以IStackType为基础实现IItemHandler的类
public class ItemUnifiedStorageHandler implements IItemHandler
{
    private DimensionsNet net;

    public ItemUnifiedStorageHandler(DimensionsNet net) {
        this.net = net;
    }

    public void onChange()
    {
        net.setDirty();
    }

    public ArrayList<IStackType> getStorage()
    {
        return this.net.getUnifiedStorage().getStorage();
    }


    @Override
    public int getSlots()
    {
        int size = 0;
        List<IStackType> storage = getStorage();
        for (int i = 0; i < storage.size(); i++) {
            IStackType stackType = storage.get(i);
            if (stackType instanceof ItemStackType) {
                size++;
            }
        }
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        int currentIndex = 0;
        List<IStackType> storage = getStorage();
        for (int i = 0; i < storage.size(); i++) {
            IStackType stackType = storage.get(i);
            if (stackType instanceof ItemStackType) {
                if(slot==currentIndex)
                {
                    return ((ItemStackType) stackType).copyStack();
                }
                else
                {
                    currentIndex++;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        ItemStackType typedStack = (ItemStackType) net.getUnifiedStorage().insert(new ItemStackType(itemStack),sim);
        return typedStack.getStack();
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        // 这个调用链分为三步
        // 1.专为物品提供的假列表中获取指定物品并转为IStackType
        // 2.使用存储器导出
        // 3.获取返回值的Stack，然后转为ItemStack再返回
        return (ItemStack) net.getUnifiedStorage().extract(new ItemStackType(getStackInSlot(slot).copyWithCount(count)),sim)
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
