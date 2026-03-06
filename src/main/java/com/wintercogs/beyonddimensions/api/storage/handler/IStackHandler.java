package com.wintercogs.beyonddimensions.api.storage.handler;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IStackHandler
{

    /**
     * 获取只读存储视图
     */
    List<KeyAmount> getStorage();

    /**
     * 当存储内容改变后，调用此方法
     * <p>
     * 请根据目的自行重写
     */
    void onChange();

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
    void clearStorage();

    /**
     * 获取指定槽位的堆叠，不要直接修改
     *
     * @param slot 槽位索引
     * @return 获取的堆叠，注意处理null
     */
    @NotNull KeyAmount getStackBySlot(int slot);

    /**
     * 根据传入的堆叠种类精确匹配（包括类型、内容、NBT，但不包括堆叠的当前数量），并返回找到的堆叠。不要直接修改
     * <p>
     * 如，传入堆叠是1个钻石，那么会返回找到的第一个钻石堆叠，不论钻石有多少个。
     *
     * @param key 目标堆叠
     * @return 找到的堆叠，注意处理null
     */
    @NotNull KeyAmount getStackByKey(IStackKey<?> key);

    /**
     * 当前存储是否存在此堆叠，精确匹配
     */
    boolean hasStack(IStackKey<?> key);

    /**
     * 直接在指定槽位设置堆叠，仅在你确定你需要的时候再使用
     */
    void setStackDirectly(int slot, IStackKey<?> key, long amount);

    /**
     * 在存储末尾添加一个堆叠，仅在你确定你需要的时候再使用
     */
    void addStackDirectly(IStackKey<?> key, long amount);

    /**
     * 尝试将指定的堆叠插入指定的槽位，并返回余量。但注意，不要修改传入的堆叠，利用副本进行操作。
     *
     * @param slot     槽位索引
     * @param key      堆叠
     * @param simulate 是否为模拟操作，如果为真，则只计算余量，不操作存储
     * @return 剩余堆叠
     */
    @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate);

    /**
     * 尝试插入指定的堆叠，直到容器所有位置被填满，然后返回剩余堆叠。不要修改传入的堆叠
     *
     * @param key      堆叠
     * @param simulate 是否为模拟操作
     * @return 剩余堆叠
     */
    @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate);

    /**
     * 尝试从指定的槽位提取出指定数量的堆叠，并返回提取的堆叠。
     * <p>
     * 此方法会在索引越界时直接返回空的ItemStackType，因此对于类型要求严格的方法。在使用其返回值时需要检测typeId或者其实例是否为空。
     *
     * @param slot     槽位索引
     * @param amount   指定的数量
     * @param simulate 是否为模拟操作
     * @return 实际能提取的堆叠
     */
    @NotNull KeyAmount extract(int slot, long amount, boolean simulate);

    /**
     * 按类型导出堆叠，并返回提取的堆叠
     *
     * @param key      堆叠类型
     * @param amount   指定的数量
     * @param simulate 是否为模拟操作
     * @param fuzzy    是否模糊匹配
     * @return 实际能提取的堆叠
     */
    @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate, boolean fuzzy);

    /**
     * 按类型导出堆叠，并返回提取的堆叠（进行精确匹配）
     *
     * @param key      堆叠类型
     * @param amount   指定的数量
     * @param simulate 是否为模拟操作
     * @return 实际能提取的堆叠
     */
    default @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate)
    {
        return extract(key, amount, simulate, false);
    }

    /**
     * 指定的槽位最大容量是多少？
     * <p>
     * 一般处理此函数时只考虑指定槽位和存储容器本身的状态，而不考虑指定槽位的内容物。如果你要按内容物限制单格存储上限，你应当修改insert方法。
     *
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
     *
     * @param slot 槽位索引
     * @param key  意图存入的堆叠
     * @return 是否能存入
     */
    boolean isStackValid(int slot, IStackKey<?> key);

    /**
     * 当前容器内是否存有物品
     */
    boolean isEmpty();
}