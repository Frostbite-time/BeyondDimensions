package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

/**
 * 无序存储实现（保留0策略）：
 * - 当数量变为0时，不移除该key；其仍占用一个槽位（在slotIndex/posMap中保留），storage中数量为0。
 * - 序列化会写出数量为0的条目（依赖基类 serializeNBT 的策略）。
 *
 * 其余行为（插入/提取/事件广播/类型桶等）沿用抽象基类。
 */
public class UnorderedStackHandlerKeepZero extends AbstractUnorderedStackHandler {

    public UnorderedStackHandlerKeepZero() {
        super(ZeroPolicy.KEEP_ZERO);
    }

    public UnorderedStackHandlerKeepZero(long slotCapacity, int slotMaxSize) {
        super(ZeroPolicy.KEEP_ZERO);
        this.slotCapacity = slotCapacity;
        this.slotMaxSize  = slotMaxSize;
    }
}