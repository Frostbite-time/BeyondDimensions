package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.stream.Collectors;

// 以IStackType为基础实现IItemHandler的类
public class ItemStackTypedHandler implements IItemHandler
{
    private DimensionsNet net;

    public ItemStackTypedHandler(DimensionsNet net) {
        this.net = net;
    }

    public ArrayList<ItemStackType> getItemOnlyStorage()
    {
        return getStorage().stream()
                .filter(stackType -> stackType instanceof ItemStackType)
                .map(stackType -> (ItemStackType) stackType)  // 关键的类型转换
                .collect(Collectors.toCollection(ArrayList::new));
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
        return getItemOnlyStorage().size();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return getItemOnlyStorage().get(slot).getStack().copy();
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
