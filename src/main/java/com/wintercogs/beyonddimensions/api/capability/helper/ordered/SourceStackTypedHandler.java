package com.wintercogs.beyonddimensions.api.capability.helper.ordered;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.SourceStackKey;
import com.wintercogs.beyonddimensions.integration.module.ars.caps.ISourceCap;
import com.wintercogs.beyonddimensions.util.BDMath;

public class SourceStackTypedHandler implements ISourceCap
{
    private final StackHandler handlerStorage;

    public SourceStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // 能否接收指定数量的魔源？能接收哪怕一点就算成功
    @Override
    public boolean canAcceptSource(int amount)
    {
        return receiveSource(amount, true) > 0;
    }

    // 能否提供指定数量的魔源？能提取哪怕一点就算成功
    @Override
    public boolean canProvideSource(int amount)
    {
        return extractSource(amount, true) > 0;
    }

    @Override
    public int getMaxExtract()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxReceive()
    {
        return Integer.MAX_VALUE;
    }

    // 汇总所有 Source 槽位的真实总量（无需快照，提前截断避免溢出）
    @Override
    public int getSource()
    {
        return handlerStorage.getBucket(SourceStackKey.ID)
                .map(bucket -> {
                    long sum = 0L;
                    final int n = bucket.size();
                    for (int i = 0; i < n; i++)
                    {
                        final int slot = bucket.get(i);
                        long amt = handlerStorage.getStackBySlot(slot).amount();
                        if (amt <= 0) continue;

                        long remain = (long) Integer.MAX_VALUE - sum;
                        if (amt >= remain)
                        {
                            return Integer.MAX_VALUE;
                        }
                        sum += amt;
                    }
                    return (int) sum;
                })
                .orElse(0);
    }

    // 返回“当前计算出来的真实最大值”：
    // eligibleSlots = Source 槽数 + 空槽数；
    // perSlot = min(原版最大堆叠量, 槽位容量[统一])；
    // capacity = eligibleSlots * perSlot
    @Override
    public int getSourceCapacity()
    {
        int sourceSlots = handlerStorage.getBucket(SourceStackKey.ID)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
        int emptySlots = handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);

        int eligibleSlots = sourceSlots + emptySlots;
        if (eligibleSlots <= 0) return 0;

        long perSlot = Math.min(
                SourceStackKey.INSTANCE.getVanillaMaxStackSize(),
                handlerStorage.getSlotCapacity(0) // 槽位容量在你的实现中对所有槽位相同
        );

        long total = perSlot * (long) eligibleSlots;
        return BDMath.clampLongToInt(total);
    }

    // getMaxSource 等同于 getSourceCapacity
    @Override
    public int getMaxSource()
    {
        return getSourceCapacity();
    }

    @Override
    public boolean canExtract()
    {
        return true;
    }

    @Override
    public boolean canReceive()
    {
        return true;
    }

    // 以“真实最大值”为上限设置目标值；只做最小差量同步（插入/抽取）
    @Override
    public void setSource(int amount)
    {
        int cap = getSourceCapacity();
        int target = Math.max(0, Math.min(amount, cap));
        int current = getSource();
        int delta = target - current;
        if (delta > 0)
        {
            handlerStorage.insert(SourceStackKey.INSTANCE, delta, false);
        }
        else if (delta < 0)
        {
            handlerStorage.extract(SourceStackKey.INSTANCE, -delta, false, false);
        }
    }

    @Override
    public void setMaxSource(int capacity)
    {
        // 不在乎这个接口
    }

    // 返回接受量
    @Override
    public int receiveSource(int amount, boolean sim)
    {
        if (amount <= 0) return 0;
        long rem = handlerStorage.insert(SourceStackKey.INSTANCE, amount, sim).amount();
        long ins = amount - rem;
        return ins <= 0 ? 0 : (int) ins;
    }

    // 返回导出量
    @Override
    public int extractSource(int amount, boolean sim)
    {
        if (amount <= 0) return 0;
        long out = handlerStorage.extract(SourceStackKey.INSTANCE, amount, sim, false).amount();
        return out <= 0 ? 0 : (int) out;
    }
}