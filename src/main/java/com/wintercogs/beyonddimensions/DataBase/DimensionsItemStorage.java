package com.wintercogs.beyonddimensions.DataBase;


import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;


public class DimensionsItemStorage implements IItemHandler
{

    private DimensionsNet net; // 用于通知维度网络进行保存
    // 实际的存储
    private final ArrayList<StoredItemStack> itemStorage;
    // 用于显示存储的list，每当发生改变时候将会

    public DimensionsItemStorage()
    {
        this.itemStorage = new ArrayList<>();
    }

    public DimensionsItemStorage(DimensionsNet net)
    {
        this.net = net;
        this.itemStorage = new ArrayList<>();
    }

    public List<StoredItemStack> getItemStorage()
    {
        return this.itemStorage;
    }

    // 添加物品到存储
    public void addItem(ItemStack itemStack, long count)
    {
        if (itemStack.isEmpty())
        {
            return;
        }
        StoredItemStack sItem = new StoredItemStack(itemStack);
        // 增加已有物品的数量 添加未有的物品
        for (StoredItemStack item : itemStorage)
        {
            if (item.equals(sItem))
            {
                item.addCount(count);
                OnChange();
                return;
            }
        }
        itemStorage.add(new StoredItemStack(itemStack, count));
        OnChange();
    }

    public void addItem(StoredItemStack storedItemStack)
    {
        if(storedItemStack == null)
        {
            return;
        }
        if (storedItemStack.getItemStack().isEmpty())
        {
            return;
        }
        for (StoredItemStack item : itemStorage)
        {
            if (item.equals(storedItemStack))
            {
                item.addCount(storedItemStack.getCount());
                OnChange();
                return;
            }
        }
        itemStorage.add(new StoredItemStack(storedItemStack));
        OnChange();
    }

    // 移除物品 并返回被移除的物品
    public ItemStack removeItem(ItemStack itemStack, long count)
    {
        StoredItemStack sItem = new StoredItemStack(itemStack);
        if (itemStorage.contains(sItem))
        {
            for (StoredItemStack item : itemStorage)
            {
                if (item.equals(sItem))
                {
                    ItemStack before_stack = item.getActualStack();
                    item.subCount(count); // subCount会将小于0的数变为0
                    if (item.getCount() == 0)
                    {
                        itemStorage.remove(item);
                        OnChange();
                        return before_stack;
                    }
                    else
                    {
                        OnChange();
                        before_stack.setCount((int) count);
                        return before_stack;
                    }
                }
            }
        }
        else
        {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeItem(int index, long count)
    {
        // 从索引中移除物品 并返回被移除的物品堆叠
        if (index > itemStorage.size())
        {
            return ItemStack.EMPTY;
        }
        StoredItemStack sItem = itemStorage.get(index);

        if (sItem != null)
        {
            //确保一次交互被移除的物品不超过该堆叠原版的最大叠加
            ItemStack before_stack;
            if (sItem.getCount() > sItem.getItemStack().getMaxStackSize())
            {
                before_stack = sItem.getVanillaMaxSizeStack();
            }
            else
            {
                before_stack = sItem.getActualStack();
            }
            if (count > before_stack.getMaxStackSize())
            {
                count = before_stack.getMaxStackSize();
            }

            // 实际执行移除的逻辑
            sItem.subCount(count); // subCount会将小于0的数变为0
            if (sItem.getCount() == 0)
            {
                itemStorage.remove(sItem);
                OnChange();
                return before_stack;
            }
            else
            {
                // 此处没有到达要移除物品本身的阈值，且前文已经移除了数量。
                OnChange();
                before_stack.setCount((int) count);
                return before_stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public StoredItemStack getStoredItemStackByIndex(int index)
    {
        if (index >= 0 && index < itemStorage.size())
        {
            return itemStorage.get(index);
        }
        else
        {
            return null;
        }
    }

    // 查询物品储存
    public StoredItemStack getStoredItemStack(StoredItemStack sItemStack)
    {
        if (itemStorage.contains(sItemStack))
        {
            for (StoredItemStack item : itemStorage)
            {
                if (item.equals(sItemStack))
                {
                    return item;
                }
            }
        }
        return null;
    }

    public boolean hasStoredItemStackType(StoredItemStack sItemStack)
    {
        if (itemStorage.contains(sItemStack))
        {
            return true;
        }
        return false;
    }

    // 将物品存储转换为 NBT 数据
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        ListTag itemsTag = new ListTag();

        itemStorage.forEach((sItem) ->
        {
            if (sItem.getItemStack() == null || sItem.getItemStack() == ItemStack.EMPTY)
            {
                return; // 在此处用于跳过空物品
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.put("ItemStack", sItem.getItemStack().save(levelRegistryAccess));
            itemTag.put("Count", LongTag.valueOf(sItem.getCount()));
            itemsTag.add(itemTag);
        });

        tag.put("Items", itemsTag);
        return tag;
    }

    // 从 NBT 数据加载物品存储
    public void deserializeNBT(HolderLookup.Provider levelRegistryAccess, CompoundTag tag)
    {

        if (tag.contains("Items", 9))
        { // 9 表示 ListTag 类型
            ListTag itemsTag = tag.getList("Items", 10); // 10 表示 CompoundTag 类型

            for (int i = 0; i < itemsTag.size(); i++)
            {
                CompoundTag itemTag = itemsTag.getCompound(i);
                ItemStack itemStack = ItemStack.parseOptional(levelRegistryAccess, itemTag.getCompound("ItemStack"));
                long count = itemTag.getLong("Count");
                addItem(itemStack, count);
            }
        }
    }

    public void OnChange()
    {
        net.setDirty();
    }

    @Override
    public int getSlots() {
        return itemStorage.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return itemStorage.get(slot).getActualStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
        if(simulate)
        {
            return ItemStack.EMPTY;
        }
        addItem(itemStack,itemStack.getCount());
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int count, boolean simulate) {
        if(simulate)
        {
            // 从索引中移除物品 并返回被移除的物品堆叠
            if (slot > itemStorage.size())
            {
                return ItemStack.EMPTY;
            }
            StoredItemStack sItem = itemStorage.get(slot);

            if (sItem != null)
            {
                //确保一次交互被移除的物品不超过该堆叠原版的最大叠加
                ItemStack before_stack;
                if (sItem.getCount() > sItem.getItemStack().getMaxStackSize())
                {
                    before_stack = sItem.getVanillaMaxSizeStack();
                }
                else
                {
                    before_stack = sItem.getActualStack();
                }
                if (count > before_stack.getMaxStackSize())
                {
                    count = before_stack.getMaxStackSize();
                }

                // 实际执行移除的逻辑
                StoredItemStack simulateItem = new StoredItemStack(sItem);
                simulateItem.subCount(count); // subCount会将小于0的数变为0
                if (simulateItem.getCount() == 0)
                {
                    return before_stack.copy();
                }
                else
                {
                    ItemStack new_stack = before_stack.copy();
                    new_stack.setCount((int) count);
                    return new_stack;
                }
            }

            return ItemStack.EMPTY;
        }
        else
        {
            return removeItem(slot,count);
        }

    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE-1;
    }

    @Override
    public boolean isItemValid(int i, ItemStack itemStack) {
        return true;
    }
}
