package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 用于{@link IStackType}接口的Handler接口，类似与{@link net.minecraft.world.item.ItemStack}与{@link net.minecraftforge.items.IItemHandler}的关系。如果你不清楚，建议先去看一看。
 * <p>
 * 在处理时尽量不涉及任何具体类型，对于需要返回空体的情况，考虑null和空的ItemStackType，后者相当通用。
 * <p>
 * 如果你需要一个可以快速定义类似原版箱子的{@link IStackType}容器，你应该继承{@link StackTypedHandler}而不是实现此接口
 */
public interface IStackTypedHandler
{

    /**
     * 获取存储列表的只读引用
     */
    List<IStackType> getStorage();

    /**
     * 当存储内容改变后，调用此方法
     * <p>
     * 请根据目的自行重写
     */
    void onChange();

    /**
     * 根据资源id返回对应的存储包装。如：ItemStackType.ID返回一个实现了IItemHandler的类实例。你需要自行保证类型安全
     * <p>
     * 这用于性能优化，以及能力注册，但是这个方法并不强制重写，如果你不打算使用，你可以直接返回null，不会有任何影响，这个方法只是一个建议
     * @param typeId 类似ItemStackType.ID
     * @return 类似一个实现了IItemHandler的类实例
     */
    Object getTypedHandler(ResourceLocation typeId);

    /**
     * 获取当前容器的槽位数量
     */
    default int getSlots()
    {
        return getStorage().size();
    }

    /**
     * 清空容器
     */
    default void clearStorage()
    {
        getStorage().clear();
    }

    /**
     * 获取指定槽位的堆叠
     * @param slot 槽位索引
     * @return 获取的堆叠，注意处理null
     */
    default IStackType getStackBySlot(int slot)
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

    /**
     * 根据传入的堆叠种类精确匹配（包括类型、内容、NBT，但不包括堆叠的当前数量），并返回找到的堆叠。
     * <p>
     * 如，传入堆叠是1个钻石，那么会返回找到的第一个钻石堆叠，不论钻石有多少个。
     * @param stackType 目标堆叠
     * @return 找到的堆叠，注意处理null
     */
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

    /**
     * 当前存储是否存在此堆叠，精确匹配
     */
    default boolean hasStackType(IStackType other)
    {
        if(getStackByStack(other) != null)
            return true;
        else
            return false;
    }

    /**
     * 直接在指定槽位设置堆叠，仅在你确定你需要的时候再使用
     */
    default void setStackDirectly(int slot,IStackType stack)
    {
        getStorage().set(slot,stack.copy());
        onChange();
    }

    /**
     * 直接在指定槽位添加一个堆叠，仅在你确定你需要的时候再使用
     */
    default void addStackToIndexDirectly(int slot , IStackType stack)
    {
        getStorage().add(slot,stack.copy());
        onChange();
    }

    /**
     * 在存储末尾添加一个堆叠，仅在你确定你需要的时候再使用
     */
    default void addStackDirectly(IStackType stack)
    {
        getStorage().add(stack.copy());
        onChange();
    }

    /**
     * 尝试将指定的堆叠插入指定的槽位，并返回余量。但注意，不要修改传入的堆叠，利用副本进行操作。
     * @param slot 槽位索引
     * @param stack 堆叠
     * @param simulate 是否为模拟操作，如果为真，则只计算余量，不操作存储
     * @return 剩余堆叠
     */
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
            maxInsert = Math.min(stack.getStackAmount(), getSlotCapacity(slot));
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
            long slotCap = Math.min(getSlotCapacity(slot),stack.getVanillaMaxStackSize());// 如需突破堆叠上限，则需要重写并移除这条语句
            maxInsert = Math.min(
                    stack.getStackAmount(),
                    slotCap - current.getStackAmount()
            );
            if (maxInsert <= 0) return stack.copy();

            remaining = stack.copyWithCount(stack.getStackAmount() - maxInsert);
            if (!simulate) {
                current.grow(maxInsert);
                onChange();
            }
        }
        return remaining;
    }

    /**
     * 尝试插入指定的堆叠，直到容器所有位置被填满，然后返回剩余堆叠。不要修改传入的堆叠
     * @param stack 堆叠
     * @param simulate 是否为模拟操作
     * @return 剩余堆叠
     */
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

    /**
     * 尝试从指定的槽位提取出指定数量的堆叠，并返回提取的堆叠。
     * <p>
     * 此方法会在索引越界时直接返回空的ItemStackType，因此对于类型要求严格的方法。在使用其返回值时需要检测typeId或者其实例是否为空。
     * @param slot 槽位索引
     * @param count 指定的数量
     * @param simulate 是否为模拟操作
     * @return 实际能提取的堆叠
     */
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


    /**
     * 按类型导出堆叠，并返回提取的堆叠
     * @param stack 堆叠类型，精确匹配
     * @param simulate 是否为模拟操作
     * @return 实际能提取的堆叠
     */
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

    /**
     * 指定的槽位最大容量是多少？
     * <p>
     * 一般处理此函数时只考虑指定槽位和存储容器本身的状态，而不考虑指定槽位的内容物。如果你要按内容物限制单格存储上限，你应当修改insert方法。
     * @param slot 槽位索引
     * @return 最大容量
     */
    long getSlotCapacity(int slot);

    /**
     * 指定的堆叠是否能插入指定的槽位？
     * <p>
     * 返回值不考虑当前容器内的实际状态。返回值只意味着，在一般情况下，该槽位是否具有对该堆叠的容纳能力。
     * <p>
     * <ul>
     *     <li>以原版容器举例，向一个已经存了64个钻石的槽位再存入一个钻石，此函数也应该返回true。</li>
     *     <li>以通用机械的化学品举例，向普通化学品储罐存入放射性化学品，无论当前储罐是什么状态，都返回false。</li>
     * </ul>
     * @param slot 槽位索引
     * @param stack 意图存入的堆叠
     * @return 是否能存入
     */
    default boolean isStackValid(int slot, IStackType stack)
    {
        return true;
    }
}
