package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;

public interface NetInterfaceAccess
{
    StackHandler getStackHandler();

    StackHandler getFakeStackHandler();

    NetInterfaceSettings getNetInterfaceSettings();

    default PopMode getPopMode()
    {
        return getNetInterfaceSettings().getPopMode();
    }

    default void setPopMode(PopMode popMode)
    {
        getNetInterfaceSettings().setPopMode(popMode);
    }

    default FuzzyMode getFuzzyMode()
    {
        return getNetInterfaceSettings().getFuzzyMode();
    }

    default void setFuzzyMode(FuzzyMode fuzzyMode)
    {
        getNetInterfaceSettings().setFuzzyMode(fuzzyMode);
    }

    RedStoneControlMode getControlMode();

    void setControlMode(RedStoneControlMode controlMode);

    default boolean canConfigurePopMode()
    {
        return true;
    }

    boolean isMenuValid();

    void onMenuDataChanged();

    static boolean transferToNet(DimensionsNet net, StackHandler stackHandler, StackHandler fakeStackHandler, int slotCount)
    {
        if (net == null) return false;
        boolean changed = false;
        for (int i = 0; i < slotCount; i++)
        {
            KeyAmount flag = fakeStackHandler.getStackBySlot(i);
            if (!flag.isEmpty() && flag.key().isSameTypeSameComponents(stackHandler.getStackBySlot(i).key()))
                continue;
            KeyAmount stack = stackHandler.getStackBySlot(i);
            if (!stack.isEmpty())
            {
                KeyAmount extracted = stackHandler.extract(i, stack.amount(), false);
                KeyAmount remaining = net.getUnifiedStorage().insert(extracted.key(), extracted.amount(), false);
                if (!remaining.isEmpty())
                    stackHandler.insert(i, remaining.key(), remaining.amount(), false);
                changed |= remaining.amount() != extracted.amount();
            }
        }
        return changed;
    }

    static boolean transferFromNet(DimensionsNet net, StackHandler stackHandler, StackHandler fakeStackHandler, int slotCount, FuzzyMode fuzzyMode)
    {
        if (net == null) return false;
        boolean changed = false;
        for (int i = 0; i < slotCount; i++)
        {
            KeyAmount flag = fakeStackHandler.getStackBySlot(i);
            if (flag.isEmpty()) continue;
            KeyAmount current = stackHandler.getStackBySlot(i);
            if (!current.isEmpty() && !current.key().isSameTypeSameComponents(flag.key()))
                continue;
            long currentAmount = current.isEmpty() ? 0 : current.amount();
            long missing = flag.key().getVanillaMaxStackSize() - currentAmount;
            if (missing <= 0) continue;
            KeyAmount stack = net.getUnifiedStorage().extract(flag.key(), missing, false, fuzzyMode == FuzzyMode.ENABLE);
            if (!stack.isEmpty())
            {
                KeyAmount remaining = stackHandler.insert(i, stack.key(), stack.amount(), false);
                if (!remaining.isEmpty())
                    net.getUnifiedStorage().insert(remaining.key(), remaining.amount(), false);
                changed |= remaining.amount() != stack.amount();
            }
        }
        return changed;
    }
}
