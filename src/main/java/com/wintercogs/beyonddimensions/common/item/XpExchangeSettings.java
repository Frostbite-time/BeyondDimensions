package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.common.machine.XpTransferSpeedMode;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class XpExchangeSettings
{
    public static final String XP_TARGET_LEVEL_TAG = "xp_target_level";
    public static final int DEFAULT_TARGET_LEVEL = 1;
    public static final int MAX_TARGET_LEVEL = 9999;

    private XpExchangeSettings()
    {
    }

    public static int sanitizeTargetLevel(int targetLevel)
    {
        return Mth.clamp(targetLevel, 0, MAX_TARGET_LEVEL);
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

    public static boolean hasTargetLevel(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains(XP_TARGET_LEVEL_TAG);
    }

    public static int getTargetLevel(ItemStack stack)
    {
        if (hasTargetLevel(stack))
        {
            return sanitizeTargetLevel(stack.getTag().getInt(XP_TARGET_LEVEL_TAG));
        }

        return targetLevelFromLegacyMode(XpExchangeItem.getOrDefaultXpTransferSpeedMode(stack, XpTransferSpeedMode.SLOW));
    }

    public static void setTargetLevel(ItemStack stack, int targetLevel)
    {
        stack.getOrCreateTag().putInt(XP_TARGET_LEVEL_TAG, sanitizeTargetLevel(targetLevel));
    }

    public static void ensureComponents(ItemStack stack)
    {
        if (!XpExchangeItem.hasXpTransferSpeedMode(stack))
            XpExchangeItem.setXpTransferSpeedMode(stack, XpTransferSpeedMode.SLOW);

        if (!XpExchangeItem.hasXpNetKeepMode(stack))
            XpExchangeItem.setXpNetKeepMode(stack, false);

        int targetLevel = getTargetLevel(stack);
        if (!hasTargetLevel(stack) || stack.getTag().getInt(XP_TARGET_LEVEL_TAG) != targetLevel)
        {
            setTargetLevel(stack, targetLevel);
        }
    }
}
