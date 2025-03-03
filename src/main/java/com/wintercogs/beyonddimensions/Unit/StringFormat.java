package com.wintercogs.beyonddimensions.Unit;

public class StringFormat
{
    // 扩展单位到 E (Exa, 10^18)
    private static final String[] UNITS = {"", "k", "M", "G", "T", "P", "E"};
    private static final long[] THRESHOLDS = {
            1_000L,          // k (10^3)
            1_000_000L,      // M (10^6)
            1_000_000_000L,  // G (10^9)
            1_000_000_000_000L,         // T (10^12)
            1_000_000_000_000_000L,     // P (10^15)
            1_000_000_000_000_000_000L  // E (10^18)
    };

    public static String formatCount(long count) {
        if (count < 1000) return String.valueOf(count);

        // 寻找最大单位
        int unitIndex = 0;
        while (unitIndex < THRESHOLDS.length - 1 && count >= THRESHOLDS[unitIndex + 1]) {
            unitIndex++;
        }

        // 计算值并格式化
        double value = count / (double) THRESHOLDS[unitIndex];
        if (value % 1 == 0) {
            return String.format("%d%s", (long) value, UNITS[unitIndex + 1]);
        }
        return String.format("%.1f%s", value, UNITS[unitIndex + 1]);
    }
}
