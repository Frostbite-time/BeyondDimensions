package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
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
        return handlerStorage.getTypeIdIndexList(ItemStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return handlerStorage.getTypeIdIndexList(ItemStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(handlerStorage::getStackBySlot)
                .map(stackType -> (ItemStack)stackType.getStack())
                .orElse(ItemStack.EMPTY);
    }

    // 这里函数的slot，是外界根据getItemOnlyStorage所认为的我们的slot
    // 故处理时需要从itemstorageindex中取值，那里记录着etItemOnlyStorage对应的索引实际对应外界索引的哪一个
    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        return handlerStorage.getTypeIdIndexList(ItemStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(actualIndex -> (ItemStack) handlerStorage.insert(actualIndex,new ItemStackType(itemStack.copy()),sim).copyStack())
                .orElse(itemStack.copy());
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        return handlerStorage.getTypeIdIndexList(ItemStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (ItemStack) handlerStorage.extract(actualIndex,count,sim).copyStack())
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return handlerStorage.getTypeIdIndexList(ItemStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (ItemStackType) handlerStorage.getStackBySlot(actualIndex))
                .map(stack -> (int)stack.getVanillaMaxStackSize())
                .orElse(99);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack itemStack)
    {
        return true;
    }
}
