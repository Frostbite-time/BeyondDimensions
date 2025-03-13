package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 以IStackType为基础实现IItemHandler的类
public class ItemUnifiedStorageHandler implements IItemHandler
{
    private DimensionsNet net;

    public ItemUnifiedStorageHandler(DimensionsNet net) {
        this.net = net;
    }

    // 获取所有为Item的槽位
    public ArrayList<ItemStackType> getItemOnlyStorage() {
        List<IStackType> storage = getStorage();
        // 预分配最大可能容量，避免扩容
        ArrayList<ItemStackType> result = new ArrayList<>(storage.size());

        for (IStackType stackType : storage) {
            if (stackType instanceof ItemStackType) {
                // 直接类型转换，无需中间操作
                result.add((ItemStackType) stackType);
            }
        }
        // 可选：释放未使用的内存（根据场景决定是否需要）
        result.trimToSize();
        return result;
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
