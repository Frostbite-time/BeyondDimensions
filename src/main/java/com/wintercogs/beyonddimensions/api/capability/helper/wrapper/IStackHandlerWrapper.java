package com.wintercogs.beyonddimensions.api.capability.helper.wrapper;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 为类似IItemHandler类所做的包装，用来动态的包装来自其他模组的handler。以便模组可以用通用的交互逻辑访问其他模组的存储。
 */
public interface IStackHandlerWrapper<T>
{

    /**
     * 处理器类型表示
     */
    ResourceLocation getTypeId();

    /**
     * 获取容器的槽位总数
     */
    int getSlots();

    /**
     * 获取对应槽位的堆叠
     *
     * @param slot 槽位索引
     * @return 堆叠
     */
    T getStackInSlot(int slot);

    /**
     * 获取指定槽位的容量
     *
     * @param slot 槽位索引
     * @return 槽位容量
     */
    long getCapacity(int slot);

    /**
     * 指定的堆叠是否允许被插入槽位
     * <p>
     * 此接口用于读取其他模组的内容，因此不要用{@link IStackHandler#isStackValid(int, IStackKey)}的描述来看待
     *
     * @param slot  槽位索引
     * @param stack 指定的堆叠
     * @return 真则为允许
     */
    boolean isStackValid(int slot, T stack);

    /**
     * 向指定的槽位，插入指定的堆叠，返回剩余量
     *
     * @param slot  槽位索引
     * @param stack 指定堆叠
     * @param sim   是否为模拟操作
     * @return 理论剩余量
     */
    long insert(int slot, T stack, boolean sim);

    /**
     * 向容器内插入指定的堆叠，返回剩余量
     *
     * @param stack 指定堆叠
     * @param sim   是否为模拟操作
     * @return 理论剩余量
     */
    long insert(T stack, boolean sim);

    /**
     * 从指定的槽位，提取指定数量的堆叠，返回提取量
     *
     * @param slot   槽位索引
     * @param amount 指定数量
     * @param sim    是否为模拟操作
     * @return 理论提取量
     */
    long extract(int slot, long amount, boolean sim);

    /**
     * 提取指定的堆叠，返回提取量
     *
     * @param stack 指定的堆叠
     * @param sim   是否为模拟操作
     * @return 理论提取量
     */
    long extract(T stack, boolean sim);

    // TODO 后续将物品容器与方块容器能力分开

    /**
     * 获取对应的物品容器，仅当包装下的容器为物品类容器时，才返回非空
     */
    default Optional<ItemStack> getContainer()
    {
        return Optional.empty();
    }
}
