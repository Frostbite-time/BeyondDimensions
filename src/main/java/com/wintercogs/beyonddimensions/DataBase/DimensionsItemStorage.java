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
    private final ArrayList<ItemStack> itemStorage;
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

    public List<ItemStack> getItemStorage()
    {
        return this.itemStorage;
    }

    // 添加物品到存储
    public void addItem(ItemStack itemStack, int count)
    {
        if (itemStack.isEmpty())
        {
            return;
        }
        // 增加已有物品的数量 添加未有的物品
        for (ItemStack itemExist : itemStorage)
        {
            if (ItemStack.isSameItemSameComponents(itemExist,itemStack))
            {
                itemExist.grow(count);
                OnChange();
                return;
            }
        }
        itemStorage.add(itemStack.copy());
        OnChange();
    }

    // 移除物品 并返回被移除的物品
    public ItemStack removeItem(ItemStack itemStack, int count)
    {
        for (ItemStack itemExist : itemStorage)
        {
            if (ItemStack.isSameItemSameComponents(itemExist,itemStack))
            {
                ItemStack before_stack = itemExist.copy();
                itemExist.shrink(count);
                if (itemExist.getCount() <= 0)
                {
                    itemStorage.remove(itemExist);
                    OnChange();
                    return before_stack;
                }
                else
                {
                    OnChange();
                    before_stack.setCount(count);
                    return before_stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeItem(int index, int count)
    {
        // 从索引中移除物品 并返回被移除的物品堆叠
        if (index > itemStorage.size())
        {
            return ItemStack.EMPTY;
        }
        ItemStack itemExist = itemStorage.get(index);

        if (itemExist != null)
        {
            //确保一次交互被移除的物品不超过该堆叠原版的最大叠加
            ItemStack before_stack;
            if (itemExist.getCount() > itemExist.getMaxStackSize())
            {
                before_stack = itemExist.copy();
                before_stack.setCount(itemExist.getMaxStackSize());
            }
            else
            {
                before_stack = itemExist.copy();
            }
            if (count > before_stack.getMaxStackSize())
            {
                count = before_stack.getMaxStackSize();
            }

            // 实际执行移除的逻辑
            itemExist.shrink(count);
            if (itemExist.getCount() <= 0)
            {
                itemStorage.remove(itemExist);
                OnChange();
                return before_stack;
            }
            else
            {
                // 此处没有到达要移除物品本身的阈值，且前文已经移除了数量。
                OnChange();
                before_stack.setCount(count);
                return before_stack;
            }
        }

        return ItemStack.EMPTY;
    }

//    public StoredItemStack getStoredItemStackByIndex(int index)
//    {
//        if (index >= 0 && index < itemStorage.size())
//        {
//            return itemStorage.get(index);
//        }
//        else
//        {
//            return null;
//        }
//    }

    public ItemStack getItemStackByIndex(int index)
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
    public ItemStack getStoredItemStack(ItemStack itemStack)
    {
        for (ItemStack itemExist : itemStorage)
        {
            if (ItemStack.isSameItemSameComponents(itemExist,itemStack))
            {
                return itemExist;
            }
        }
        return null;
    }

    public boolean hasItemStackType(ItemStack itemStack)
    {
        for (ItemStack itemExist : itemStorage)
        {
            if (ItemStack.isSameItemSameComponents(itemExist,itemStack))
            {
                return true;
            }
        }
        return false;
    }

    // 将物品存储转换为 NBT 数据
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        ListTag itemsTag = new ListTag();

        itemStorage.forEach((item) ->
        {
            if (item == null || item == ItemStack.EMPTY)
            {
                return; // 在此处用于跳过空物品
            }
            CompoundTag itemTag = new CompoundTag();
            itemTag.put("ItemStack", item.save(levelRegistryAccess));
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
                addItem(itemStack, (int) count);
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
        return itemStorage.get(slot).copy();
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
            ItemStack itemStack = itemStorage.get(slot);

            if (itemStack != null)
            {
                //确保一次交互被移除的物品不超过该堆叠原版的最大叠加
                ItemStack before_stack;
                if (itemStack.getCount() > itemStack.getMaxStackSize())
                {
                    before_stack = itemStack.copy();
                    before_stack.setCount(itemStack.getMaxStackSize());
                }
                else
                {
                    before_stack = itemStack.copy();
                }
                if (count > before_stack.getMaxStackSize())
                {
                    count = before_stack.getMaxStackSize();
                }

                // 实际执行移除的逻辑
                ItemStack simulateItem = itemStack.copy();
                simulateItem.shrink(count);
                if (simulateItem.getCount() <= 0)
                {
                    return before_stack.copy();
                }
                else
                {
                    ItemStack new_stack = before_stack.copy();
                    new_stack.setCount(count);
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
    public boolean isItemValid(int slot, ItemStack itemStack) {
        return true;
    }
}
