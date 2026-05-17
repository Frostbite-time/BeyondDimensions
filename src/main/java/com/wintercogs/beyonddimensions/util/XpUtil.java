package com.wintercogs.beyonddimensions.util;

import net.minecraft.world.entity.player.Player;

public class XpUtil
{
    /**
     * 读取玩家的“等级 + 进度”，转换为一个 double（例如 35.4）。
     */
    public static double levelAsDouble(Player player)
    {
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;
        return level + (double) progress;
    }

    /**
     * 计算两个“带小数的等级”之间的经验差（toLevel - fromLevel），返回 long。
     * <p>如果任一输入为负，则返回 0。</p>
     * <p>四舍五入到最近的整数经验。</p>
     */
    public static long xpBetweenLevels(double fromLevel, double toLevel)
    {
        if (fromLevel < 0 || toLevel < 0)
        {
            System.err.println("[XpUtils] Level must be non-negative.");
            return 0L;
        }
        double diff = totalXpAtFractionalLevel(toLevel) - totalXpAtFractionalLevel(fromLevel);
        return Math.round(diff);
    }

    // ====== 内部辅助方法 ======

    /**
     * 指定整数等级 L（到达该等级为止），从 0 级累计所需总经验。L >= 0
     */
    private static long totalXpAtIntegerLevel(int L)
    {
        if (L <= 0) return 0L;

        if (L <= 16)
        {
            // T(L) = L^2 + 6L
            return (long) L * L + 6L * L;
        }
        else if (L <= 31)
        {
            // T(L) = (5L^2 - 81L + 720) / 2  —— 用整数算式避免浮点误差
            long num = 5L * L * L - 81L * L + 720L;
            return num / 2L;
        }
        else
        {
            // T(L) = (9L^2 - 325L + 4440) / 2
            long num = 9L * L * L - 325L * L + 4440L;
            return num / 2L;
        }
    }

    /**
     * 从 fromLevel 补到“至少 targetLevel（整数级，进度>=0）”需要的 XP（向上取整）。
     */
    public static long xpToReachAtLeast(double fromLevel, int targetLevel)
    {
        if (fromLevel < 0 || targetLevel < 0) return 0L;
        double diff = totalXpAtIntegerLevel(targetLevel) - totalXpAtFractionalLevel(fromLevel);
        if (diff <= 0) return 0L;
        return (long) Math.ceil(diff - 1e-9); // epsilon 防止 0.999999 被误判
    }

    /**
     * 当前相比于“targetLevel 的 0 进度”多出来的 XP（向下取整），用于抽走。
     */
    public static long xpExcessAbove(double fromLevel, int targetLevel)
    {
        if (fromLevel < 0 || targetLevel < 0) return 0L;
        double diff = totalXpAtFractionalLevel(fromLevel) - totalXpAtIntegerLevel(targetLevel);
        if (diff <= 0) return 0L;
        return (long) Math.floor(diff + 1e-9); // epsilon
    }

    /**
     * 从等级 L 升到 L+1 需要的经验（L 为整数等级）。
     */
    private static int xpCostToNextLevel(int L)
    {
        if (L <= 15)
        {
            return 2 * L + 7;
        }
        else if (L <= 30)
        {
            return 5 * L - 38;
        }
        else
        {
            return 9 * L - 158;
        }
    }

    /**
     * 带小数等级 x（x>=0）到达为止的累计总经验（double）。
     */
    private static double totalXpAtFractionalLevel(double x)
    {
        // 拆成整数部分 + 小数部分
        int base = (int) Math.floor(x);
        double frac = x - base; // [0,1)
        long baseTotal = totalXpAtIntegerLevel(base);
        int cost = xpCostToNextLevel(base);
        // 小数部分按当前等级到下一等级的线性比例折算
        return baseTotal + frac * cost;
    }
}
