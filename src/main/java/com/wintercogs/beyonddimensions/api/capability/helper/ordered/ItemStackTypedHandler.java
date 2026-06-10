package com.wintercogs.beyonddimensions.api.capability.helper.ordered;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * 仅基于桶（Item 桶 + Empty 桶）进行索引映射的 IItemHandler 视图。
 * - 不做快照/镜像；单次调用内直接访问桶当前状态。
 * - 可视槽位 = ItemStackKey 桶槽位 + EmptyStackKey 桶槽位（顺序：先 Item，后 Empty）。
 * - getStackInSlot 直接使用缓存对象并设数量（高频无拷贝）；若缓存为空/为空栈则返回 EMPTY。
 */
public class ItemStackTypedHandler implements IItemHandler, IItemHandlerModifiable
{
    private static final ResourceLocation ITEM_TYPE = ItemStackKey.ID;

    private final StackHandler handlerStorage;

    public ItemStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // ---- 小工具：纯 Optional 计算，避免 orElse(null) ----

    /**
     * Item 桶的槽位数量（非空的 ItemStackKey 槽）。
     */
    private int itemCount()
    {
        return handlerStorage.getBucket(ITEM_TYPE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 空桶的槽位数量（EmptyStackKey 槽）。
     */
    private int emptyCount()
    {
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 可视槽位是否处于 Item 区域（非 Empty 区域）。
     */
    private boolean inItemRegion(int visibleSlot)
    {
        int items = itemCount();
        return visibleSlot >= 0 && visibleSlot < items;
    }

    /**
     * 取 Item 桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getItemSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(ITEM_TYPE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    /**
     * 取空桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getEmptySlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    /**
     * 将“可视槽位”映射为真实槽位；无效则 -1。
     */
    private int resolveActualIndex(int visibleSlot)
    {
        if (visibleSlot < 0) return -1;
        int items = itemCount();
        if (visibleSlot < items)
        {
            return getItemSlotAt(visibleSlot);
        }
        int rest = visibleSlot - items;
        return getEmptySlotAt(rest);
    }

    // ---- IItemHandler ----

    /**
     * 可视槽位 = Item 桶 + 空桶；全空时也会 > 0。
     */
    @Override
    public int getSlots()
    {
        return itemCount() + emptyCount();
    }

    /**
     * 高频：直接使用缓存对象并设数量。
     * 若缓存不存在/为空，或该槽处于空区域，则返回 ItemStack.EMPTY。
     */
    @Override
    public @NotNull ItemStack getStackInSlot(int slot)
    {
        if (!inItemRegion(slot)) return ItemStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return ItemStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return ItemStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof ItemStack item)) return ItemStack.EMPTY;
        if (item.isEmpty()) return ItemStack.EMPTY; // 避免对空栈 setCount

        int shown = BDMath.clampLongToInt(ka.amount());
        if (shown <= 0) return ItemStack.EMPTY;

        item.setCount(shown); // 直接改缓存数量并返回
        return item;
    }

    /**
     * 直接设置对应槽位物品
     */
    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack)
    {
        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return;

        if (stack.isEmpty())
        {
            handlerStorage.setStackDirectly(actualIndex, EmptyStackKey.INSTANCE, 0);
            return;
        }

        handlerStorage.setStackDirectly(actualIndex, new ItemStackKey(stack), stack.getCount());
    }

    /**
     * 插入：可落在空槽区或 Item 区，内部 insert 会做校验与容量约束。
     */
    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return stack.copy(); // 槽位无效：全部剩余

        KeyAmount rem = handlerStorage.insert(actualIndex, new ItemStackKey(stack), stack.getCount(), simulate);
        Object leftover = rem.toStack();
        return (leftover instanceof ItemStack is) ? is : stack.copy(); // 与源断开
    }

    /**
     * 仅 Item 区可抽取；空槽区恒 EMPTY。
     */
    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount <= 0) return ItemStack.EMPTY;
        if (!inItemRegion(slot)) return ItemStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return ItemStack.EMPTY;

        KeyAmount taken = handlerStorage.extract(actualIndex, amount, simulate);
        Object out = taken.toStack();
        return (out instanceof ItemStack is) ? is : ItemStack.EMPTY;
    }

    /**
     * 空槽区返回默认 99；Item 区返回 min(物品上限, 槽位容量)。
     */
    @Override
    public int getSlotLimit(int slot)
    {
        if (!inItemRegion(slot)) return 99;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return 99;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        long byType = (ka.isEmpty())
                ? 99
                : ka.key().getVanillaMaxStackSize();
        long byCap = handlerStorage.getSlotCapacity(actualIndex);
        return BDMath.clampLongToInt(Math.min(byType, byCap));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        // 放宽；最终由 insert 决定
        return true;
    }
}
