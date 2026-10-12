package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.function.Predicate;

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

    /**
     * 能否修改面配置与主动弹出；动态结构里的接口没有固定的相邻方块，不能修改
     */
    default boolean canConfigureSides()
    {
        return true;
    }

    @Override
    default boolean isSettingAvailable(MachineSetting<?> setting)
    {
        return canConfigureSides() || !MachineSides.onSidePage(machineType(), setting);
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
        // 配置禁止的资源大类按最终进入网络的资源判断，规则转换出的资源同样受限
        Predicate<IStackKey<?>> admits = ServerConfigRuntime.hasInterfaceInputTypeRestrictions()
                ? key -> !ServerConfigRuntime.isInterfaceInputTypeBlocked(key.getTypeId())
                : null;
        boolean changed = false;
        for (int i = 0; i < slotCount; i++)
        {
            KeyAmount flag = fakeStackHandler.getStackBySlot(i);
            if (!flag.isEmpty() && flag.key().isSameTypeSameComponents(stackHandler.getStackBySlot(i).key()))
                continue;
            KeyAmount stack = stackHandler.getStackBySlot(i);
            if (stack.isEmpty())
                continue;
            // 余量总是格子里的资源：先放进网络，再从格子里扣掉放进去的数量
            long accepted = stack.amount() - net.getUnifiedStorage().insert(stack.key(), stack.amount(), false, admits).amount();
            if (accepted > 0)
            {
                stackHandler.extract(i, accepted, false);
                changed = true;
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
            int slot = i;
            // 格子里已有资源时只补同一种，空格子才按模糊匹配挑变体
            KeyAmount moved = net.getUnifiedStorage().extractInto(flag.key(), missing, current.isEmpty() && fuzzyMode == FuzzyMode.ENABLE,
                    offer -> offer.amount() - stackHandler.insert(slot, offer.key(), offer.amount(), true).amount(),
                    taken -> stackHandler.insert(slot, taken.key(), taken.amount(), false).amount());
            changed |= !moved.isEmpty();
        }
        return changed;
    }
}
