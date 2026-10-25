package com.wintercogs.beyonddimensions.common.menu.state;

import com.wintercogs.beyonddimensions.common.init.BDNetDataTypes;
import com.wintercogs.beyonddimensions.common.init.BDNetTasks;

import java.util.Arrays;

/**
 * 网络能量最近一段时间的记录：每刻记下存量，按 {@link #BAR_TICKS} 刻一格汇总，保留最近 {@link #BARS} 格。
 * 作为只在内存中的挂接数据 {@link BDNetDataTypes#ENERGY_HISTORY} 挂在网络上，由 {@link BDNetTasks} 的每刻任务推进
 */
public final class EnergyHistory
{
    /**
     * 每格汇总的刻数
     */
    public static final int BAR_TICKS = 10;

    /**
     * 保留的格数
     */
    public static final int BARS = 64;

    // 最近速率按这么多刻平均
    private static final int RECENT_TICKS = 20;

    // 各格内存量变化的总和与各格结束时的存量，环形存放，head 是下一格的位置
    private final long[] flows = new long[BARS];
    private final long[] stored = new long[BARS];
    private int head;
    private int count;
    private long completed;

    // 正在汇总的一格
    private long barFlow;
    private int barTicks;

    // 最近每刻的变化，按刻数环形存放
    private final long[] recent = new long[RECENT_TICKS];
    private int recentCount;
    private long ticks;

    private long last;
    private boolean sampled;

    // 上一次取出的快照，有新的一格后才重建
    private Bars snapshot = Bars.EMPTY;
    private long snapshotCompleted;

    /**
     * 汇总好的各格，从旧到新：{@code flows} 是每格内存量变化的总和，{@code stored} 是每格结束时的存量
     */
    public record Bars(long[] flows, long[] stored)
    {
        public static final Bars EMPTY = new Bars(new long[0], new long[0]);

        @Override
        public boolean equals(Object other)
        {
            return other instanceof Bars bars && Arrays.equals(flows, bars.flows) && Arrays.equals(stored, bars.stored);
        }

        @Override
        public int hashCode()
        {
            return 31 * Arrays.hashCode(flows) + Arrays.hashCode(stored);
        }
    }

    /**
     * 记下这一刻的存量，每刻调用一次
     */
    public void record(long now)
    {
        long change = sampled ? saturatedSubtract(now, last) : 0;
        last = now;
        sampled = true;
        recent[(int) (ticks++ % RECENT_TICKS)] = change;
        recentCount = Math.min(recentCount + 1, RECENT_TICKS);
        barFlow = saturatedAdd(barFlow, change);
        if (++barTicks < BAR_TICKS)
            return;
        flows[head] = barFlow;
        stored[head] = now;
        head = (head + 1) % BARS;
        count = Math.min(count + 1, BARS);
        completed++;
        barFlow = 0;
        barTicks = 0;
    }

    /**
     * 最近一秒平均每刻的变化，正数为流入
     */
    public double recentRate()
    {
        if (recentCount == 0)
            return 0;
        double sum = 0;
        for (int i = 0; i < recentCount; i++)
            sum += recent[i];
        return sum / recentCount;
    }

    /**
     * 已汇总完的各格
     */
    public Bars bars()
    {
        if (snapshotCompleted != completed)
        {
            long[] flowCopy = new long[count];
            long[] storedCopy = new long[count];
            for (int i = 0; i < count; i++)
            {
                int index = Math.floorMod(head - count + i, BARS);
                flowCopy[i] = flows[index];
                storedCopy[i] = stored[index];
            }
            snapshot = new Bars(flowCopy, storedCopy);
            snapshotCompleted = completed;
        }
        return snapshot;
    }

    private static long saturatedAdd(long a, long b)
    {
        long sum = a + b;
        // 同号相加却变了号就是溢出
        if (((a ^ sum) & (b ^ sum)) < 0)
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        return sum;
    }

    private static long saturatedSubtract(long a, long b)
    {
        long difference = a - b;
        if (((a ^ b) & (a ^ difference)) < 0)
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        return difference;
    }
}
