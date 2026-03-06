package com.wintercogs.beyonddimensions.api.storage.handler.impl;

/**
 * 无序存储实现（到0即删除策略）：
 * - 当数量降至0时，从 storage/索引 中移除该键并释放槽位。
 * - 序列化不写出数量<=0的条目（依赖基类 serializeNBT 的策略）。
 * <p>
 * 绝大多数逻辑由抽象基类按 ZeroPolicy 执行，无需覆写。
 */
public class UnorderedStackHandlerRemoveZero extends AbstractUnorderedStackHandler
{

    public UnorderedStackHandlerRemoveZero(UiTimestampPolicy uiTimestampPolicy)
    {
        super(ZeroPolicy.REMOVE_ON_ZERO, uiTimestampPolicy);
    }

    public UnorderedStackHandlerRemoveZero(UiTimestampPolicy uiTimestampPolicy, long slotCapacity, int slotMaxSize)
    {
        super(ZeroPolicy.REMOVE_ON_ZERO, uiTimestampPolicy);
        this.slotCapacity = slotCapacity;
        this.slotMaxSize = slotMaxSize;
    }
}