package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerWrapper implements IStackHandlerWrapper<ItemStack>
{
    private final IItemHandler itemHandler;

    public ItemHandlerWrapper(Object itemHandler)
    {
        this.itemHandler = (IItemHandler) itemHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return ItemStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return itemHandler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return itemHandler.getSlotLimit(slot);
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack)
    {
        return itemHandler.isItemValid(slot, stack);
    }

    @Override
    public long insert(int slot, ItemStack Stack, boolean sim)
    {
        return itemHandler.insertItem(slot, Stack, sim).getCount();
    }

    @Override
    public long insert(ItemStack stack, boolean sim)
    {
        // 遍历每个槽位进行插入
        for(int i = 0; i < getSlots(); i++)
        {
            stack = itemHandler.insertItem(i, stack, sim);
            if(stack.isEmpty())
                return 0;
        }
        return stack.getCount();
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        return itemHandler.extractItem(slot,(int)Math.min(amount,Integer.MAX_VALUE),sim).getCount();
    }

    @Override
    public long extract(ItemStack stack, boolean sim)
    {
        int currentNum = 0;
        //遍历每个对应槽位进行提取
        //最后返回实际提取的副本
        for (int i = 0; i < getSlots(); i++)
        {
            if(ItemStack.isSameItemSameTags(itemHandler.getStackInSlot(i), stack))
            {
                int extracting = itemHandler.extractItem(i,stack.getCount(),sim).getCount();
                stack.shrink(extracting);
                currentNum += extracting;
                if(stack.isEmpty())
                    break;
            }
        }
        return currentNum;
    }
}
