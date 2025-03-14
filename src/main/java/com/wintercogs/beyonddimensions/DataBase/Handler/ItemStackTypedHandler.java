package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// 用于实现StackTypedHandler转向IItemHandler的类
public class ItemStackTypedHandler implements IItemHandler
{
    private StackTypedHandler handlerStorage;
    private List<Integer> itemStorageIndex = new ArrayList<>(); //存储了ItemOnlyStorage的原Index对应，每次调用getItemOnlyStorage实时更新

    public ItemStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    // 获取所有可用于插入Item的槽位
    public List<ItemStackType> getItemOnlyStorage() {
        itemStorageIndex.clear();
        List<IStackType> storage = getStorage();

        // 第一次遍历：收集所有符合条件的索引 即空位置和符合类型的位置都可以用来插入当前类型
        for (int i = 0; i < storage.size(); i++) {
            IStackType stackType = storage.get(i);
            if (stackType.isEmpty() || stackType instanceof ItemStackType) {
                itemStorageIndex.add(i);
            }
        }

        // 根据已知大小初始化ArrayList，避免扩容
        List<ItemStackType> result = new ArrayList<>(itemStorageIndex.size());

        // 第二次遍历：填充结果列表
        for (int i = 0; i < itemStorageIndex.size(); i++) {
            int index = itemStorageIndex.get(i);
            IStackType stackType = storage.get(index);
            if (stackType.isEmpty()) {
                result.add(new ItemStackType());
            } else {
                result.add((ItemStackType) stackType);
            }
        }

        return result;
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

    // 这里函数的slot，是外界根据getItemOnlyStorage所认为的我们的slot
    // 故处理时需要从itemstorageindex中取值，那里记录着etItemOnlyStorage对应的索引实际对应外界索引的哪一个
    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
    {
        getItemOnlyStorage(); // 更新索引
        ItemStackType remaining = (ItemStackType) handlerStorage.insert(itemStorageIndex.get(slot),new ItemStackType(itemStack.copy()),sim);
        return remaining.copyStack();
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean sim)
    {
        getItemOnlyStorage(); // 更新索引
        ItemStackType extracts = (ItemStackType) handlerStorage.extract(itemStorageIndex.get(slot),count,sim);
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
