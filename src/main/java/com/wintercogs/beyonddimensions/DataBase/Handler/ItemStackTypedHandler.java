package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

// 用于实现StackTypedHandler转向IItemHandler的类
public class ItemStackTypedHandler implements IItemHandler
{
    private StackTypedHandler handlerStorage;

    public ItemStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getSlots()
    {
        List<Integer> slots = handlerStorage.getTypeIdIndexList(ItemStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(ItemStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (ItemStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return ItemStack.EMPTY;
    }

    // 这里函数的slot，是外界根据getItemOnlyStorage所认为的我们的slot
    // 故处理时需要从itemstorageindex中取值，那里记录着etItemOnlyStorage对应的索引实际对应外界索引的哪一个
    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(ItemStackType.ID).get(slot);
        ItemStackType remaining = (ItemStackType) handlerStorage.insert(actualIndex,new ItemStackType(itemStack.copy()),sim);
        return remaining.copyStack();
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(ItemStackType.ID).get(slot);
        ItemStackType extracts = (ItemStackType) handlerStorage.extract(actualIndex,count,sim);
        return extracts.copyStack();
    }

    @Override
    public int getSlotLimit(int slot)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(ItemStackType.ID).get(slot);
        ItemStackType stackType = (ItemStackType)handlerStorage.getStackBySlot(actualIndex);
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
