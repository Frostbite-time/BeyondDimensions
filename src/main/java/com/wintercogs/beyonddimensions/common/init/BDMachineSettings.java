package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.SettingEditor;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.machine.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.wintercogs.beyonddimensions.api.machine.MachineSetting.lowercase;

/**
 * BD 机器的设置；附属的机器也可以直接使用它们
 */
public final class BDMachineSettings
{
    public static final DeferredRegister<MachineSetting<?>> SETTINGS = DeferredRegister.create(BDRegistries.MACHINE_SETTING_KEY, BDConstants.MODID);

    private static final String MACHINE = "ui.beyonddimensions.machine.";
    private static final String SIDE = "ui.beyonddimensions.side.";
    private static final List<String> RANGE_NAMES = List.of("lowest", "low", "mid", "high", "highest", "chunk");

    /**
     * 方块机器的红石控制
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<RedStoneControlMode>> REDSTONE = SETTINGS.register("redstone",
            () -> MachineSetting.enumeration(RedStoneControlMode.class, RedStoneControlMode.IGNORE)
                    .label(MACHINE + "redstone")
                    .editor(SettingEditor.choice(List.of(RedStoneControlMode.values()), mode -> MACHINE + "redstone." + lowercase(mode)))
                    .legacyKey("control_mode")
                    .build());

    /**
     * 物品机器的总开关：物品感知不到红石，只有工作与停止
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<RedStoneControlMode>> WORKING = SETTINGS.register("working",
            () -> MachineSetting.enumeration(RedStoneControlMode.class, RedStoneControlMode.IGNORE)
                    .label(MACHINE + "working")
                    .editor(SettingEditor.toggle(RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING))
                    .legacyComponent(BDDataComponents.CONTROL_MODE)
                    .build());

    /**
     * 是否把内容推送给相邻的容器
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<PopMode>> POP = SETTINGS.register("pop",
            () -> MachineSetting.enumeration(PopMode.class, PopMode.STOP)
                    .label(MACHINE + "pop")
                    .editor(SettingEditor.toggle(PopMode.OPEN, PopMode.STOP))
                    .legacyKey("pop_mode")
                    .legacyKey("popMode")
                    // 更早的版本以布尔值保存
                    .legacy(tag -> tag.contains("popMode", Tag.TAG_BYTE)
                            ? Optional.of(tag.getBoolean("popMode") ? PopMode.OPEN : PopMode.STOP)
                            : Optional.empty())
                    .build());

    /**
     * 按物品种类匹配，不比较组件
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<FuzzyMode>> FUZZY = SETTINGS.register("fuzzy",
            () -> MachineSetting.enumeration(FuzzyMode.class, FuzzyMode.DISABLE)
                    .label(MACHINE + "fuzzy")
                    .hint(MACHINE + "fuzzy.hint")
                    .editor(SettingEditor.toggle(FuzzyMode.ENABLE, FuzzyMode.DISABLE))
                    .legacyKey("fuzzy_mode")
                    .legacyComponent(BDDataComponents.FUZZY_MODE)
                    .build());

    /**
     * 过滤槽是否按种类匹配、不比较组件；默认开启，过滤槽一直是这样匹配的
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<FuzzyMode>> FILTER_FUZZY = SETTINGS.register("filter_fuzzy",
            () -> MachineSetting.enumeration(FuzzyMode.class, FuzzyMode.ENABLE)
                    .label(MACHINE + "fuzzy")
                    .hint(MACHINE + "fuzzy.hint")
                    .editor(SettingEditor.toggle(FuzzyMode.ENABLE, FuzzyMode.DISABLE))
                    .build());

    /**
     * 过滤槽的用法：忽略、白名单或黑名单
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<FilterMode>> FILTER = SETTINGS.register("filter",
            () -> MachineSetting.enumeration(FilterMode.class, FilterMode.BLACK)
                    .label(MACHINE + "filter")
                    .editor(SettingEditor.choice(List.of(FilterMode.values()), mode -> MACHINE + "filter." + lowercase(mode)))
                    .legacyKey("filter_type")
                    .legacyComponent(BDDataComponents.FILTER_MODE)
                    .build());

    /**
     * 是否把产物（补货器：与模板不符的物品）收入维度网络
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<ReceiveMode>> RECEIVE = SETTINGS.register("receive",
            () -> MachineSetting.enumeration(ReceiveMode.class, ReceiveMode.STOP)
                    .label("ui.beyonddimensions.furnace.receive")
                    .hint("ui.beyonddimensions.furnace.receive.hint")
                    .editor(SettingEditor.toggle(ReceiveMode.OPEN, ReceiveMode.STOP))
                    .legacyKey("receive_mode")
                    .legacyComponent(BDDataComponents.RECEIVE_MODE)
                    .build());

    /**
     * 是否均匀分配各列的原料
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<AutoSortMode>> SORT = SETTINGS.register("sort",
            () -> MachineSetting.enumeration(AutoSortMode.class, AutoSortMode.STOP)
                    .label("ui.beyonddimensions.furnace.sorting")
                    .hint("ui.beyonddimensions.furnace.sorting.hint")
                    .editor(SettingEditor.toggle(AutoSortMode.OPEN, AutoSortMode.STOP))
                    .legacyKey("sort_mode")
                    .build());

    public static final DeferredHolder<MachineSetting<?>, MachineSetting<HopperItemMode>> COLLECT_ITEMS = SETTINGS.register("collect_items",
            () -> MachineSetting.enumeration(HopperItemMode.class, HopperItemMode.ALLOW)
                    .label(MACHINE + "collect.items")
                    .editor(SettingEditor.toggle(HopperItemMode.ALLOW, HopperItemMode.DENY))
                    .legacyKey("hopper_item_model")
                    .legacyComponent(BDDataComponents.HOPPER_ITEM_MODE)
                    .build());

    public static final DeferredHolder<MachineSetting<?>, MachineSetting<HopperXpMode>> COLLECT_EXPERIENCE = SETTINGS.register("collect_experience",
            () -> MachineSetting.enumeration(HopperXpMode.class, HopperXpMode.DENY)
                    .label(MACHINE + "collect.experience")
                    .editor(SettingEditor.toggle(HopperXpMode.ALLOW, HopperXpMode.DENY))
                    .legacyKey("hopper_xp_mode")
                    .legacyComponent(BDDataComponents.HOPPER_XP_MODE)
                    .build());

    public static final DeferredHolder<MachineSetting<?>, MachineSetting<HopperFluidMode>> COLLECT_FLUIDS = SETTINGS.register("collect_fluids",
            () -> MachineSetting.enumeration(HopperFluidMode.class, HopperFluidMode.DENY)
                    .label(MACHINE + "collect.fluids")
                    .editor(SettingEditor.toggle(HopperFluidMode.ALLOW, HopperFluidMode.DENY))
                    .legacyKey("hopper_fluid_mode")
                    .legacyComponent(BDDataComponents.HOPPER_FLUID_MODE)
                    .build());

    public static final DeferredHolder<MachineSetting<?>, MachineSetting<HopperNBTMode>> COLLECT_COMPONENTS = SETTINGS.register("collect_components",
            () -> MachineSetting.enumeration(HopperNBTMode.class, HopperNBTMode.DENY)
                    .label(MACHINE + "collect.components")
                    .editor(SettingEditor.toggle(HopperNBTMode.ALLOW, HopperNBTMode.DENY))
                    .legacyKey("hopper_nbt_mode")
                    .legacyComponent(BDDataComponents.HOPPER_NBT_MODE)
                    .build());

    /**
     * 收集范围；各机器的实际范围不同，选项说明由机器类型给出
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<HopperRangeMode>> RANGE = SETTINGS.register("range",
            () -> MachineSetting.enumeration(HopperRangeMode.class, HopperRangeMode.RADIUS_MID)
                    .label(MACHINE + "range")
                    .editor(SettingEditor.choice(List.of(HopperRangeMode.values()), mode -> MACHINE + "range." + rangeName(mode)).cycling())
                    .legacyKey("hopper_range_mode")
                    .legacyComponent(BDDataComponents.HOPPER_RANGE_MODE)
                    .build());

    /**
     * 喂食器何时喂食
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<FeederMode>> FEEDING = SETTINGS.register("feeding",
            () -> MachineSetting.enumeration(FeederMode.class, FeederMode.NORMAL)
                    .label("ui.beyonddimensions.feeder.mode")
                    .editor(new SettingEditor.Choice<>(List.of(FeederMode.values()),
                            mode -> "ui.beyonddimensions.feeder.mode." + lowercase(mode),
                            mode -> "ui.beyonddimensions.feeder.mode." + lowercase(mode) + ".hint", true))
                    .legacyComponent(BDDataComponents.FEEDER_MODE)
                    .build());

    /**
     * 磁铁吸取的物品送到哪里；默认只送网络，旧磁铁的去向不变
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<PickupTarget>> PICKUP_TARGET = SETTINGS.register("pickup_target",
            () -> MachineSetting.enumeration(PickupTarget.class, PickupTarget.NETWORK_ONLY)
                    .label(MACHINE + "pickup_target")
                    .editor(new SettingEditor.Choice<>(List.of(PickupTarget.values()),
                            target -> MACHINE + "pickup_target." + lowercase(target),
                            target -> MACHINE + "pickup_target." + lowercase(target) + ".hint", true))
                    .build());

    /** 磁铁的背包保留过滤；默认只保留标记的物品，不影响旧磁铁的行为 */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<FilterMode>> INVENTORY_KEEP = SETTINGS.register("inventory_keep",
            () -> MachineSetting.enumeration(FilterMode.class, FilterMode.WHITE)
                    .label(MACHINE + "inventory_keep")
                    .editor(new SettingEditor.Choice<>(List.of(FilterMode.values()),
                            mode -> MACHINE + "inventory_keep." + lowercase(mode),
                            mode -> MACHINE + "inventory_keep." + lowercase(mode) + ".hint", false))
                    .build());

    /**
     * 存量发信器的发信条件
     */
    public static final DeferredHolder<MachineSetting<?>, MachineSetting<EmitMode>> EMIT = SETTINGS.register("emit",
            () -> MachineSetting.enumeration(EmitMode.class, EmitMode.AT_LEAST)
                    .label("ui.beyonddimensions.emitter.mode")
                    .editor(SettingEditor.choice(List.of(EmitMode.values()), mode -> "ui.beyonddimensions.emitter.mode." + lowercase(mode)))
                    .build());

    /**
     * 六个面各自的输入输出，见 {@link MachineSides}
     */
    private static final Map<Direction, DeferredHolder<MachineSetting<?>, MachineSetting<SideMode>>> SIDES = new EnumMap<>(Direction.class);

    static
    {
        for (Direction side : Direction.values())
            SIDES.put(side, SETTINGS.register("side_" + side.getName(), () -> sideSetting(side)));
    }

    private BDMachineSettings()
    {
    }

    /**
     * 朝 side 的那一面的输入输出
     */
    public static DeferredHolder<MachineSetting<?>, MachineSetting<SideMode>> side(Direction side)
    {
        return SIDES.get(side);
    }

    private static MachineSetting<SideMode> sideSetting(Direction side)
    {
        return MachineSetting.enumeration(SideMode.class, SideMode.BOTH)
                .label(SIDE + side.getName())
                .editor(new SettingEditor.Choice<>(List.of(SideMode.values()),
                        mode -> SIDE + "mode." + lowercase(mode),
                        mode -> SIDE + "mode." + lowercase(mode) + ".hint", false))
                .build();
    }

    /**
     * 收集范围在语言键里的写法
     */
    public static String rangeName(HopperRangeMode mode)
    {
        return RANGE_NAMES.get(mode.ordinal());
    }

    public static void register(IEventBus modBus)
    {
        SETTINGS.register(modBus);
    }
}
