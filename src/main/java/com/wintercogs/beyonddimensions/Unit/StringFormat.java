package com.wintercogs.beyonddimensions.Unit;

public class StringFormat
{
    public static String formatCount(int count) {
        if (count >= 1000000000) {  // 十亿
            return String.format("%.1fb", count / 1000000000.0);
        } else if (count >= 1000000) {  // 百万
            return String.format("%.1fm", count / 1000000.0);
        } else if (count >= 1000) {  // 千
            return String.format("%.1fk", count / 1000.0);
        } else {
            return Integer.toString(count);  // 小于1000时直接返回
        }
    }
}
