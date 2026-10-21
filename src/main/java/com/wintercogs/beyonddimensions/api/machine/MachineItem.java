package com.wintercogs.beyonddimensions.api.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 物品机器：设置保存在物品 NBT 的 {@link #SETTINGS_TAG} 下，
 * 用 {@link #machine} 得到对应的 {@link Machine}
 */
public interface MachineItem
{
    /**
     * 物品 NBT 中保存设置的键
     */
    String SETTINGS_TAG = "machine_settings";

    MachineType machineType();

    /**
     * 堆叠的设置。旧版本的物品把每项设置存在 NBT 的独立键下，没有新设置时从它们读出
     */
    default MachineSettings settings(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SETTINGS_TAG, Tag.TAG_COMPOUND))
            return MachineSettings.fromTag(tag.getCompound(SETTINGS_TAG));
        MachineSettings legacy = MachineSettings.EMPTY;
        for (MachineType.Entry<?> entry : machineType().settings())
            legacy = readLegacy(entry.setting(), stack, legacy);
        return legacy;
    }

    /**
     * 写回堆叠，同时移除这类机器旧版本的设置键
     */
    default void setSettings(ItemStack stack, MachineSettings settings)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(SETTINGS_TAG, settings.toTag());
        for (MachineType.Entry<?> entry : machineType().settings())
            for (String legacy : entry.setting().legacyItemKeys())
                tag.remove(legacy);
    }

    default boolean isSettingAvailable(ItemStack stack, MachineSetting<?> setting)
    {
        return true;
    }

    default void exportConfig(ItemStack stack, CompoundTag config)
    {
        config.put(Machine.SETTINGS, Machine.exportSettings(machineType(), settings(stack)).toTag());
    }

    default void applyConfig(ItemStack stack, CompoundTag config)
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
        public void exportConfig(CompoundTag config)
        {
            item.exportConfig(stack, config);
        }

        @Override
        public void applyConfig(CompoundTag config)
        {
            item.applyConfig(stack, config);
        }
    }
}
