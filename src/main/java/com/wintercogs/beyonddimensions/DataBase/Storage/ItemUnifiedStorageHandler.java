package com.wintercogs.beyonddimensions.DataBase.Storage;

import java.util.ArrayList;
import java.util.List;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

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
        List<Integer> slots = net.getUnifiedStorage().getTypeIdIndexList(ItemStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = net.getUnifiedStorage().getTypeIdIndexList(ItemStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (ItemStack)net.getUnifiedStorage().getStorage().get(actualIndex).copyStack();
        }
        else return ItemStack.EMPTY;
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
