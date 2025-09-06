package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

// 用于实现StackTypedHandler转向IItemHandler的类
public class ItemStackTypedHandler implements IItemHandler
{
    private final StackHandler handlerStorage;

    public ItemStackTypedHandler(StackHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getSlots()
    {
        return handlerStorage.getBucket(ItemStackKey.ID)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return handlerStorage.getBucket(ItemStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(handlerStorage::getStackBySlot)
                .map(stack -> {
                    Object outStack = handlerStorage.getOutStackByKey(stack.key());
                    if(outStack instanceof ItemStack itemStack)
                    {
                        if(!itemStack.isEmpty())
                            itemStack.setCount(BDMath.clampLongToInt(stack.amount()));
                        return itemStack;
                    }
                    return null;
                })
                .orElse(ItemStack.EMPTY);
    }

    // 这里函数的slot，是外界根据getItemOnlyStorage所认为的我们的slot
    // 故处理时需要从itemstorageindex中取值，那里记录着etItemOnlyStorage对应的索引实际对应外界索引的哪一个
    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        return handlerStorage.getBucket(ItemStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(actualIndex -> {
                    if(handlerStorage.insert(actualIndex,new ItemStackKey(itemStack), itemStack.getCount(),sim).toStack() instanceof ItemStack outStack)
                        return outStack;
                    else
                        return null;
                })
                .orElse(itemStack.copy()); // 确保返回值与源断开联系
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        return handlerStorage.getBucket(ItemStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> {
                    if(handlerStorage.extract(actualIndex,count,sim).toStack() instanceof ItemStack outStack)
                        return outStack;
                    else
                        return null;
                })
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return handlerStorage.getBucket(ItemStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> BDMath.clampLongToInt(stack.key().getVanillaMaxStackSize()))
                .orElse(99);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack itemStack)
    {
        return true;
    }

}
