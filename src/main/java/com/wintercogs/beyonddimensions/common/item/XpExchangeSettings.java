package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.XpTransferSpeedMode;
import net.minecraft.world.item.ItemStack;

public final class XpExchangeSettings
{
    public static final int DEFAULT_TARGET_LEVEL = 1;
    public static final int MAX_TARGET_LEVEL = 9999;

    private XpExchangeSettings()
    {
    }

    public static int sanitizeTargetLevel(int targetLevel)
    {
        return Math.clamp(targetLevel, 0, MAX_TARGET_LEVEL);
    }

    public static int targetLevelFromLegacyMode(XpTransferSpeedMode legacyMode)
    {
        return switch (legacyMode)
        {
            case SLOW -> 1;
            case MID -> 10;
            case HIGH -> 30;
            case HIGHEST -> 100;
            case OVER_HIGHEST -> 150;
        };
    }

    public static int getTargetLevel(ItemStack stack)
    {
        if (stack.has(BDDataComponents.XP_TARGET_LEVEL))
            return sanitizeTargetLevel(stack.getOrDefault(BDDataComponents.XP_TARGET_LEVEL, DEFAULT_TARGET_LEVEL));

        return targetLevelFromLegacyMode(stack.getOrDefault(BDDataComponents.XP_TRANSFER_SPEED_MODE, XpTransferSpeedMode.SLOW));
    }

    public static void setTargetLevel(ItemStack stack, int targetLevel)
    {
        stack.set(BDDataComponents.XP_TARGET_LEVEL, sanitizeTargetLevel(targetLevel));
    }

    public static void ensureComponents(ItemStack stack)
    {
        if (!stack.has(BDDataComponents.XP_NET_KEEP_MODE))
            stack.set(BDDataComponents.XP_NET_KEEP_MODE, false);

        int targetLevel = getTargetLevel(stack);
        if (!stack.has(BDDataComponents.XP_TARGET_LEVEL)
                || stack.getOrDefault(BDDataComponents.XP_TARGET_LEVEL, DEFAULT_TARGET_LEVEL) != targetLevel)
        {
            stack.set(BDDataComponents.XP_TARGET_LEVEL, targetLevel);
        }
    }
}
