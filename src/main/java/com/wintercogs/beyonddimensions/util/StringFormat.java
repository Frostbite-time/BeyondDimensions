package com.wintercogs.beyonddimensions.util;

public class StringFormat
{
    private static final String[] UNITS = {"", "k", "M", "G", "T", "P", "E"};
    private static final long[] THRESHOLDS = {
            1_000L,                     // k (10^3)
            1_000_000L,                 // M (10^6)
            1_000_000_000L,             // G (10^9)
            1_000_000_000_000L,         // T (10^12)
            1_000_000_000_000_000L,     // P (10^15)
            1_000_000_000_000_000_000L  // E (10^18)
    };

    /**
     * 格式化给定数字
     */
    public static String formatCount(long count)
    {
        if (count < 1000) return String.valueOf(count);

        // 寻找最大单位
        int unitIndex = 0;
        while (unitIndex < THRESHOLDS.length - 1 && count >= THRESHOLDS[unitIndex + 1])
        {
            unitIndex++;
        }

        // 计算值并格式化
        double value = count / (double) THRESHOLDS[unitIndex];
        return String.format("%d%s", (long) value, UNITS[unitIndex + 1]);
    }

    /**
     * 与{@link StringFormat#formatCount(long)}一致，但是会将传入的count除以1000。用于流体单位计算
     */
    public static String formatBucket(long count)
    {
        if (count < 1000) return String.valueOf(count / 1000f);

        count = count / 1000;

        if (count < 1000) return String.valueOf(count);

        int unitIndex = 0;
        while (unitIndex < THRESHOLDS.length - 1 && count >= THRESHOLDS[unitIndex + 1])
        {
            unitIndex++;
        }

        double value = count / (double) THRESHOLDS[unitIndex];
        return String.format("%d%s", (long) value, UNITS[unitIndex + 1]);
    }

    /**
     * 为能量增减等情况使用的格式化
     */
    public static String formatChange(long change)
    {
        if (change == 0)
        {
            return "0";
        }

        String sign = change > 0 ? "+" : "-";
        long absValue = Math.abs(change);

        // 特殊处理小于1000的值（直接显示原始值）
        if (absValue < 1000)
        {
            return sign + String.format("%d", absValue);
        }

        // 寻找匹配的单位
        int unitIndex = 0;
        while (unitIndex < THRESHOLDS.length - 1 && absValue >= THRESHOLDS[unitIndex + 1])
        {
            unitIndex++;
        }

        // 计算带单位的值并格式化
        double value = absValue / (double) THRESHOLDS[unitIndex];
        return sign + String.format("%.2f%s", value, UNITS[unitIndex + 1]);
    }
}
