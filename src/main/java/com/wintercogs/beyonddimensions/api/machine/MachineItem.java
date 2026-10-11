package com.wintercogs.beyonddimensions.api.machine;

import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 物品机器：设置保存在物品堆叠的 {@link BDDataComponents#MACHINE_SETTINGS} 组件上，
 * 用 {@link #machine} 得到对应的 {@link Machine}
 */
public interface MachineItem
{
    MachineType machineType();

    /**
     * 堆叠的设置。旧版本的物品把每项设置存在独立的组件里，没有新组件时从它们读出
     */
    default MachineSettings settings(ItemStack stack)
    {
        MachineSettings settings = stack.get(BDDataComponents.MACHINE_SETTINGS.get());
        if (settings != null)
            return settings;
        MachineSettings legacy = MachineSettings.EMPTY;
        for (MachineType.Entry<?> entry : machineType().settings())
            legacy = readLegacy(entry.setting(), stack, legacy);
        return legacy;
    }

    /**
     * 写回堆叠，同时移除这类机器旧版本的设置组件
     */
    default void setSettings(ItemStack stack, MachineSettings settings)
    {
        stack.set(BDDataComponents.MACHINE_SETTINGS.get(), settings);
        for (MachineType.Entry<?> entry : machineType().settings())
            for (DataComponentType<?> legacy : entry.setting().legacyComponents())
                stack.remove(legacy);
    }

    default boolean isSettingAvailable(ItemStack stack, MachineSetting<?> setting)
    {
        return true;
    }

    default void exportConfig(ItemStack stack, CompoundTag config, HolderLookup.Provider registries)
    {
        config.put(Machine.SETTINGS, Machine.exportSettings(machineType(), settings(stack)).toTag());
    }

    default void applyConfig(ItemStack stack, CompoundTag config, HolderLookup.Provider registries)
    {
        Machine machine = machine(stack);
        MachineSettings next = Machine.applySettings(machine, config);
        if (!next.equals(settings(stack)))
            setSettings(stack, next);
    }

    /**
     * 这个堆叠作为一台机器
     */
    default Machine machine(ItemStack stack)
    {
        return new StackMachine(this, stack);
    }

    private static <T> MachineSettings readLegacy(MachineSetting<T> setting, ItemStack stack, MachineSettings into)
    {
        return setting.readLegacy(stack).map(value -> into.with(setting, value)).orElse(into);
    }

    /**
     * 物品堆叠作为机器：读写都直接作用在堆叠上
     */
    record StackMachine(MachineItem item, ItemStack stack) implements Machine
    {
        @Override
        public MachineType machineType()
        {
            return item.machineType();
        }

        @Override
        public MachineSettings settings()
        {
            return item.settings(stack);
        }

        @Override
        public void setSettings(MachineSettings settings)
        {
            item.setSettings(stack, settings);
        }

        @Override
        public boolean isSettingAvailable(MachineSetting<?> setting)
        {
            return item.isSettingAvailable(stack, setting);
        }

        @Override
        public void exportConfig(CompoundTag config, HolderLookup.Provider registries)
        {
            item.exportConfig(stack, config, registries);
        }

        @Override
        public void applyConfig(CompoundTag config, HolderLookup.Provider registries)
        {
            item.applyConfig(stack, config, registries);
        }
    }
}
