package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

// 用于实现 StackTypedHandler 转向 IItemHandler 的类
public class ItemStackTypedHandler implements IItemHandler, IItemHandlerModifiable
{

    private final StackTypedHandler handlerStorage;

    public ItemStackTypedHandler(StackTypedHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 将统一存储中的所有槽位视为潜在的物品槽位。
     */
    @Override
    public int getSlots()
    {
        return handlerStorage.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot)
    {
        if (slot < 0 || slot >= handlerStorage.getSlots())
        {
            return ItemStack.EMPTY;
        }

        IStackType<?> stack = handlerStorage.getStackBySlot(slot);
        if (stack instanceof ItemStackType itemStackType && !itemStackType.isEmpty())
        {
            return (ItemStack) itemStackType.copyStack();
        }

        // 槽位不是 ItemStackType 或为空，视为 EMPTY
        return ItemStack.EMPTY;
    }

    // 这里函数的slot，是外界根据 IItemHandler 所认为的我们的 slot
    // 现在语义为：slot 直接对应统一存储的同索引槽位
    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate)
    {
        if (itemStack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        if (slot < 0 || slot >= handlerStorage.getSlots())
        {
            return itemStack.copy();
        }

        // 统一存储会处理空占位 -> ItemStackType 以及索引更新
        IStackType<?> remainingStack = handlerStorage.insert(
                slot,
                new ItemStackType(itemStack.copy()),
                simulate
        );

        long remaining = remainingStack.getStackAmount();
        if (remaining <= 0)
        {
            return ItemStack.EMPTY;
        }
        // 剩余量封装回 ItemStack 返回
        return (ItemStack) remainingStack.copyStack();
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int count, boolean simulate)
    {
        if (slot < 0 || slot >= handlerStorage.getSlots() || count <= 0)
        {
            return ItemStack.EMPTY;
        }

        IStackType<?> current = handlerStorage.getStackBySlot(slot);
        if (!(current instanceof ItemStackType) || current.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        IStackType<?> extracted = handlerStorage.extract(slot, count, simulate);
        if (extracted instanceof ItemStackType itemExtract && !itemExtract.isEmpty())
        {
            return itemExtract.copyStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        if (slot < 0 || slot >= handlerStorage.getSlots())
        {
            return 0;
        }

        IStackType<?> stack = handlerStorage.getStackBySlot(slot);
        if (stack instanceof ItemStackType itemStackType)
        {
            // 使用该槽位当前物品类型的原版最大堆叠上限
            return (int) itemStackType.getVanillaMaxStackSize();
        }

        // 槽位目前不是物品类型时，给一个合理的默认值-99也是原版实现的值
        return 99;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack itemStack)
    {
        return true;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack itemStack)
    {
    }
}
