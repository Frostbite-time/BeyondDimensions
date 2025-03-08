package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;

import java.util.List;

// 用于IStackType接口的Handler接口
// 在处理时尽量不涉及任何具体类型
// 无法为null的空体可以返回ItemStackType()
public interface IStackTypedHandler
{

    // 获取用于存储的列表的直接引用
    List<IStackType> getStorage();

    // 当存储实际变动时候执行的方法，没有特殊情况可以为空体
    void onChange();

    // 获取当前槽位总数
    default int getSlots()
    {
        return getStorage().size();
    }

    // 获取对应槽位的Stack
    default IStackType getStackInSlot(int slot)
    {
        if (slot >= 0 && slot < getStorage().size())
        {
            return getStorage().get(slot).copy();
        }
        else
        {
            return null;
        }
    }

    default IStackType getStackByStack(IStackType stackType)
    {
        for (IStackType existing : getStorage())
        {
            if (existing.getTypeId().equals(stackType.getTypeId()))
            {
                if(existing.isSameTypeSameComponents(stackType))
                    return existing.copy();
            }
        }
        return null;
    }

    default boolean hasStackType(IStackType other)
    {
        if(getStackByStack(other) != null)
            return true;
        else
            return false;
    }

    // 尝试将指定的堆叠插入指定的槽位，并返回剩余堆叠
    default IStackType insert(int slot, IStackType stack, boolean simulate)
    {
        List<IStackType> storage = getStorage();
        // 检查槽位有效性
        if (slot < 0 || slot >= storage.size()) {
            return stack.copy();
        }
        // 检查堆叠有效性
        if (!isStackValid(slot, stack) || stack.isEmpty()) {
            return stack.copy();
        }

        IStackType current = storage.get(slot);
        long maxInsert;
        IStackType remaining;

        if (current == null || current.isEmpty()) {
            // 空槽位：创建新堆叠
            maxInsert = Math.min(stack.getStackAmount(), getSlotLimit(slot));
            maxInsert = Math.min(maxInsert,stack.getVanillaMaxStackSize()); // 如需突破堆叠上限，则需要重写并移除这条语句
            if (maxInsert <= 0) return stack.copy();

            remaining = stack.copyWithCount(stack.getStackAmount() - maxInsert);
            if (!simulate) {
                IStackType newStack = stack.copyWithCount(maxInsert);
                storage.set(slot, newStack);
                onChange();
            }
        } else {
            // 已有堆叠：检查类型一致性
            if (!current.isSameTypeSameComponents(stack)) {
                return stack.copy();
            }
            // 计算可插入量
            maxInsert = Math.min(
                    stack.getStackAmount(),
                    getSlotLimit(slot) - current.getStackAmount()
            );
            maxInsert = Math.min(maxInsert,stack.getVanillaMaxStackSize()); // 如需突破堆叠上限，则需要重写并移除这条语句
            if (maxInsert <= 0) return stack.copy();

            remaining = stack.copyWithCount(stack.getStackAmount() - maxInsert);
            if (!simulate) {
                current.grow(maxInsert);
                onChange();
            }
        }
        return remaining;
    }

    // 尝试插入指定的堆叠，直到容器所有位置被填满，然后返回剩余堆叠
    default IStackType insert(IStackType stack, boolean simulate)
    {
        IStackType remaining = stack.copy();

        // 第一阶段：合并现有堆叠
        for (int slot = 0; slot < getSlots(); slot++) {
            IStackType current = getStorage().get(slot);
            if (!current.isEmpty() && current.isSameTypeSameComponents(stack)) {
                remaining = insert(slot, remaining, simulate);
                if (remaining.isEmpty()) break;
            }
        }

        // 第二阶段：填充空槽位
        if (!remaining.isEmpty()) {
            for (int slot = 0; slot < getSlots(); slot++) {
                IStackType current = getStorage().get(slot);
                if (current.isEmpty()) {
                    remaining = insert(slot, remaining, simulate);
                    if (remaining.isEmpty()) break;
                }
            }
        }

        return remaining;
    }

    // 尝试从指定的槽位提取出指定数量的堆叠，并返回提取的堆叠
    // 对于类型要求严格的方法。在使用其返回值时需要检测typeId或者其实例
    default IStackType extract(int slot, long count, boolean simulate)
    {
        List<IStackType> storage = getStorage();
        if (slot < 0 || slot >= storage.size()) {
            return new ItemStackType(); // 以不带参数ItemStackType作为空体
        }

        IStackType current = storage.get(slot);
        if (current.isEmpty()) {
            return current.getEmpty();
        }

        long extractable = Math.min(count, current.getStackAmount());
        IStackType extracted = current.copyWithCount(extractable);

        if (!simulate) {
            current.shrink(extractable);
            if (current.isEmpty()) {
                storage.set(slot, current.getEmpty());
            }
            onChange();
        }

        return extracted;
    }

    // 按类型导出堆叠，并返回提取的堆叠
    default IStackType extract(IStackType stack, boolean simulate)
    {
        IStackType result = stack.getEmpty();
        long remaining = stack.getStackAmount();

        // 遍历所有槽位提取匹配的堆叠
        for (int slot = 0; slot < getSlots(); slot++) {
            IStackType current = getStorage().get(slot);
            if (current.isEmpty() || !current.isSameTypeSameComponents(stack)) {
                continue;
            }

            // 计算可提取量
            long available = current.getStackAmount();
            long toExtract = Math.min(remaining, available);
            if (toExtract <= 0) continue;

            // 执行提取操作
            IStackType extracted = extract(slot, toExtract, simulate);
            if (!extracted.isEmpty()) {
                if (result.isEmpty()) {
                    result = extracted;
                } else {
                    result.grow(extracted.getStackAmount());
                }
                remaining -= extracted.getStackAmount();
                if (remaining <= 0) break;
            }
        }

        return result.copyWithCount(stack.getStackAmount() - remaining);
    }

    // 指定的槽位的最大容量是多少？
    long getSlotLimit(int slot);

    // 指定的堆叠是否能插入指定的槽位
    // 返回值不考虑当前容器内的实际状态。
    // 返回值只意味着，在没有任何意外的情况下，该槽位是否具有对该堆叠的容纳能力
    // 一般而言为true
    default boolean isStackValid(int slot, IStackType stack)
    {
        return true;
    }
}
