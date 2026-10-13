package com.wintercogs.beyonddimensions.api.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

/**
 * 一台机器：所属的机器类型与它的设置。设置页、菜单同步与配置卡都通过这个接口操作机器
 */
public interface Machine extends Configurable
{
    /**
     * 配置里保存设置的键
     */
    String SETTINGS = "settings";

    MachineType machineType();

    MachineSettings settings();

    /**
     * 替换设置并保存：方块实体标记修改，物品写回组件
     */
    void setSettings(MachineSettings settings);

    /**
     * 这项设置此刻能否修改
     */
    default boolean isSettingAvailable(MachineSetting<?> setting)
    {
        return true;
    }

    /**
     * 这项设置的值；没有存过时取机器类型给的默认值
     */
    default <T> T setting(MachineSetting<T> setting)
    {
        MachineType.Entry<T> entry = machineType().entry(setting);
        return entry != null ? entry.valueIn(settings()) : settings().get(setting);
    }

    default <T> void setSetting(MachineSetting<T> setting, T value)
    {
        MachineSettings current = settings();
        MachineSettings next = current.with(setting, value);
        if (next != current)
            setSettings(next);
    }

    @Override
    default void exportConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        config.put(SETTINGS, exportSettings(machineType(), settings()).toTag());
    }

    @Override
    default void applyConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        MachineSettings next = applySettings(this, config);
        if (!next.equals(settings()))
            setSettings(next);
    }

    /**
     * 写到配置卡上的设置：每项设置都写出当前的值
     */
    static MachineSettings exportSettings(MachineType type, MachineSettings settings)
    {
        MachineSettings exported = MachineSettings.EMPTY;
        for (MachineType.Entry<?> entry : type.settings())
            exported = withCurrent(entry, settings, exported);
        return exported;
    }

    private static <T> MachineSettings withCurrent(MachineType.Entry<T> entry, MachineSettings from, MachineSettings into)
    {
        return into.with(entry.setting(), entry.valueIn(from));
    }

    /**
     * 把配置里这类机器的、此刻可修改且合法的设置合并进 machine 当前的设置
     */
    static MachineSettings applySettings(Machine machine, CompoundTag config)
    {
        MachineSettings next = machine.settings();
        if (!config.contains(SETTINGS, Tag.TAG_COMPOUND))
            return next;
        MachineSettings card = MachineSettings.fromTag(config.getCompound(SETTINGS));
        for (MachineType.Entry<?> entry : machine.machineType().settings())
            next = copy(entry, card, next, machine);
        return next;
    }

    private static <T> MachineSettings copy(MachineType.Entry<T> entry, MachineSettings from, MachineSettings into, Machine machine)
    {
        MachineSetting<T> setting = entry.setting();
        if (!from.has(setting) || !machine.isSettingAvailable(setting))
            return into;
        T value = from.get(setting);
        return entry.accepts(value) && !Objects.equals(entry.valueIn(into), value) ? into.with(setting, value) : into;
    }
}
