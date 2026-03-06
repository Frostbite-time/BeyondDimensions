package com.wintercogs.beyonddimensions.integration.module.ifs.storage;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.resources.ResourceLocation;

public class WardenSoulStackTypedHandler implements ISoulHandler
{

    private static final ResourceLocation SOUL_TYPE = WardenSoulStackKey.ID;

    private final StackHandler handlerStorage;

    public WardenSoulStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // ---------- 工具：纯 Optional，返回基本类型/哨兵 ----------

    /**
     * 幽匿之魂桶的槽位数量（非空的 WardenSoulStackKey 槽）。
     */
    private int soulCount()
    {
        return handlerStorage.getBucket(SOUL_TYPE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 空桶（EmptyStackKey）的槽位数量。
     */
    private int emptyCount()
    {
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 可视槽位是否处于“魂区域”（非 Empty 区域）。
     */
    private boolean inSoulRegion(int visibleSlot)
    {
        int souls = soulCount();
        return visibleSlot >= 0 && visibleSlot < souls;
    }

    /**
     * 取魂桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getSoulSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(SOUL_TYPE)
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
        int souls = soulCount();
        if (visibleSlot < souls)
        {
            return getSoulSlotAt(visibleSlot);
        }
        int rest = visibleSlot - souls;
        return getEmptySlotAt(rest);
    }

    // ================= ISoulHandler =================

    /**
     * 可视槽位 = 幽匿之魂桶 + 空桶。
     */
    @Override
    public int getSoulTanks()
    {
        return soulCount() + emptyCount();
    }

    /**
     * 空区返回 0；非空区直接返回 KeyAmount 的数量（不限制到容量）。
     */
    @Override
    public int getSoulInTank(int slot)
    {
        if (!inSoulRegion(slot)) return 0;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return 0;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return 0;

        int shown = BDMath.clampLongToInt(ka.amount());
        return Math.max(shown, 0);
    }

    /**
     * 使用真实槽位的容量；空区同样有真实槽位可查询。
     */
    @Override
    public int getTankCapacity(int slot)
    {
        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return 0;
        return BDMath.clampLongToInt(handlerStorage.getSlotCapacity(actualIndex));
    }

    /**
     * 聚合填充：不区分槽位，交给底层合并。
     */
    @Override
    public int fill(int amount, Action action)
    {
        if (amount <= 0) return 0;
        long remaining = handlerStorage.insert(WardenSoulStackKey.INSTANCE, amount, action.simulate()).amount();
        return BDMath.clampLongToInt(amount - remaining);
    }

    /**
     * 聚合抽取：不区分槽位。
     */
    @Override
    public int drain(int amount, Action action)
    {
        if (amount <= 0) return 0;
        long taken = handlerStorage.extract(WardenSoulStackKey.INSTANCE, amount, action.simulate(), false).amount();
        return BDMath.clampLongToInt(taken);
    }
}

