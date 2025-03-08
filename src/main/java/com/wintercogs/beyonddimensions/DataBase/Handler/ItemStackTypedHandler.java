package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 用于实现StackTypedHandler转向IItemHandler的类
public class ItemStackTypedHandler implements IItemHandler
{
    private StackTypedHandler handlerStorage;

    public ItemStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    // 获取所有可用于插入Item的槽位
    public List<ItemStackType> getItemOnlyStorage()
    {
        return getStorage().stream()
                .filter(stackType -> {
                    if(stackType.isEmpty())
                        return true;
                    if(stackType instanceof ItemStackType)
                        return true;
                    return false;
                })
                .map(stackType -> {
                    if(stackType.isEmpty())
                        return new ItemStackType(); // 返回空体
                    else
                        return (ItemStackType) stackType;
                })  // 关键的类型转换
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<IStackType> getStorage()
    {
        return this.handlerStorage.getStorage();
    }

    @Override
    public int getSlots()
    {
        return getItemOnlyStorage().size();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return getItemOnlyStorage().get(slot).copyStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        ItemStackType remaining = (ItemStackType) handlerStorage.insert(slot,new ItemStackType(itemStack.copy()),sim);
        return remaining.copyStack();
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        ItemStackType extracts = (ItemStackType) handlerStorage.extract(slot,count,sim);
        return extracts.copyStack();
    }

    @Override
    public int getSlotLimit(int slot)
    {
        ItemStackType stackType = getItemOnlyStorage().get(slot);
        if(!stackType.isEmpty())
        {
            return (int) stackType.getVanillaMaxStackSize();
        }
        else
        {
            return 99;
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack)
    {
        return true;
    }
}
