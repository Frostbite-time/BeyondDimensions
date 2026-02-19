package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

// 以IStackType为基础实现IItemHandler的类
public class ItemUnifiedStorageHandler implements IItemHandler
{
    private final UnifiedStorage storage;

    public ItemUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getSlots()
    {
        // 默认返回长度比实际大1，可以让其他模组不因为存储长度而无法插入物品
        // 此类封装性良好，只需内部方法对使用的索引进行二次检查，即可避免NPE问题
        // 最后，UnifiedStorage实际并无槽位数限制且自动合并同类物品，除了读取信息和提取指定槽位物品都无需索引参与，对于超出索引的读取返回EMPTY即可
        // 所以，这样做是安全的
        return storage.getBucket(ItemStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        return storage.getBucket(ItemStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if (outStack instanceof ItemStack itemStack)
                    {
                        if (!itemStack.isEmpty())
                            itemStack.setCount(BDMath.clampLongToInt(storage.getStackByKey(key).amount()));
                        return itemStack;
                    }
                    return null;
                })
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack itemStack, boolean sim)
    {
        KeyAmount remaining = storage.insert(new ItemStackKey(itemStack), itemStack.getCount(), sim);
        if (!remaining.isEmpty() && remaining.toStack() instanceof ItemStack stack)
            return stack;
        return ItemStack.EMPTY; // 无剩余则返回空
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int count, boolean sim)
    {
        return storage.getBucket(ItemStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> storage.extract(key, count, sim, false))
                .filter(keyAmount -> !keyAmount.isEmpty())
                .map(keyAmount -> {
                    Object outStack = keyAmount.toStack();
                    if (outStack instanceof ItemStack itemStack)
                        return itemStack;
                    return ItemStack.EMPTY;
                })
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack itemStack)
    {
        return true;
    }
}
