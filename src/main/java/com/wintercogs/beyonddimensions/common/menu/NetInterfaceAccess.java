package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.machine.BDMachineSettings;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.item.MatterCompressionBall;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 网络接口：方块，以及 Create 动态结构里的接口
 */
public interface NetInterfaceAccess extends Machine
{
    String FLAGS = "flags";

    StackHandler getStackHandler();

    StackHandler getFakeStackHandler();

    @Override
    default MachineType machineType()
    {
        return BDMachineTypes.NET_INTERFACE.get();
    }

    default PopMode getPopMode()
    {
        return setting(BDMachineSettings.POP.get());
    }

    default void setPopMode(PopMode popMode)
    {
        setSetting(BDMachineSettings.POP.get(), popMode);
    }

    default FuzzyMode getFuzzyMode()
    {
        return setting(BDMachineSettings.FUZZY.get());
    }

    default void setFuzzyMode(FuzzyMode fuzzyMode)
    {
        setSetting(BDMachineSettings.FUZZY.get(), fuzzyMode);
    }

    default RedStoneControlMode getControlMode()
    {
        return setting(BDMachineSettings.REDSTONE.get());
    }

    default void setControlMode(RedStoneControlMode controlMode)
    {
        setSetting(BDMachineSettings.REDSTONE.get(), controlMode);
    }

    default boolean canConfigurePopMode()
    {
        return true;
    }

    @Override
    default boolean isSettingAvailable(MachineSetting<?> setting)
    {
        return setting != BDMachineSettings.POP.get() || canConfigurePopMode();
    }

    @Override
    default void exportConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        Machine.super.exportConfig(config, registries);
        config.put(FLAGS, getFakeStackHandler().serializeNBT(registries));
    }

    @Override
    default void applyConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        Machine.super.applyConfig(config, registries);
        if (config.contains(FLAGS, Tag.TAG_COMPOUND))
            getFakeStackHandler().deserializeNBT(registries, config.getCompound(FLAGS));
        onMenuDataChanged();
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
            if (!stack.isEmpty() && !isInputTypeBlocked(stack.key(), 0))
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

    private static boolean isInputTypeBlocked(IStackKey<?> key, int depth)
    {
        if (!ServerConfigRuntime.hasInterfaceInputTypeRestrictions())
        {
            return false;
        }
        if (ServerConfigRuntime.isInterfaceInputTypeBlocked(key.getTypeId()))
        {
            return true;
        }
        if (!(key instanceof ItemStackKey itemKey)
                || !(itemKey.getSource() instanceof MatterCompressionBall))
        {
            return false;
        }
        if (depth >= 16)
        {
            return true;
        }

        ItemStack ballStack = itemKey.copyStack();
        List<KeyAmount> contents = ballStack.getOrDefault(BDDataComponents.ISTACK_SLOTS, List.of());
        for (KeyAmount content : contents)
        {
            if (content != null && !content.isEmpty() && isInputTypeBlocked(content.key(), depth + 1))
            {
                return true;
            }
        }
        return false;
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
