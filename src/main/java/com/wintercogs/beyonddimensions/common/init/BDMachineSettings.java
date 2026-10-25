package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.api.machine.SettingEditor;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import com.wintercogs.beyonddimensions.common.machine.RelativeSide;
import com.wintercogs.beyonddimensions.common.machine.setting.*;
import net.minecraft.nbt.Tag;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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
    public static final RegistryObject<MachineSetting<RedStoneControlMode>> REDSTONE = SETTINGS.register("redstone",
            () -> MachineSetting.enumeration(RedStoneControlMode.class, RedStoneControlMode.IGNORE)
                    .label(MACHINE + "redstone")
                    .editor(SettingEditor.buttons(List.of(RedStoneControlMode.values()), mode -> MACHINE + "redstone." + lowercase(mode)))
                    .legacyKey("control_mode")
                    .build());

    /**
     * 物品机器的总开关：物品感知不到红石，只有工作与停止
     */
    public static final RegistryObject<MachineSetting<RedStoneControlMode>> WORKING = SETTINGS.register("working",
            () -> MachineSetting.enumeration(RedStoneControlMode.class, RedStoneControlMode.IGNORE)
                    .label(MACHINE + "working")
                    .editor(SettingEditor.toggle(RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING))
                    .legacyItemKey("control_mode")
                    .build());

    /**
     * 是否把内容推送给相邻的容器
     */
    public static final RegistryObject<MachineSetting<PopMode>> POP = SETTINGS.register("pop",
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
     * 按物品种类匹配，不比较 NBT
     */
    public static final RegistryObject<MachineSetting<FuzzyMode>> FUZZY = SETTINGS.register("fuzzy",
            () -> MachineSetting.enumeration(FuzzyMode.class, FuzzyMode.DISABLE)
                    .label(MACHINE + "fuzzy")
                    .hint(MACHINE + "fuzzy.hint")
                    .editor(SettingEditor.toggle(FuzzyMode.ENABLE, FuzzyMode.DISABLE))
                    .legacyKey("fuzzy_mode")
                    .legacyItemKey("fuzzy_mode")
                    .build());

    /**
     * 过滤槽是否按种类匹配、不比较 NBT；默认开启，过滤槽一直是这样匹配的
     */
    public static final RegistryObject<MachineSetting<FuzzyMode>> FILTER_FUZZY = SETTINGS.register("filter_fuzzy",
            () -> MachineSetting.enumeration(FuzzyMode.class, FuzzyMode.ENABLE)
                    .label(MACHINE + "fuzzy")
                    .hint(MACHINE + "fuzzy.hint")
                    .editor(SettingEditor.toggle(FuzzyMode.ENABLE, FuzzyMode.DISABLE))
                    .build());

    /**
     * 过滤槽的用法：忽略、白名单或黑名单
     */
    public static final RegistryObject<MachineSetting<FilterMode>> FILTER = SETTINGS.register("filter",
            () -> MachineSetting.enumeration(FilterMode.class, FilterMode.BLACK)
                    .label(MACHINE + "filter")
                    .editor(SettingEditor.buttons(List.of(FilterMode.values()), mode -> MACHINE + "filter." + lowercase(mode)))
                    .legacyKey("filter_type")
                    .legacyItemKey("filter_mode")
                    .build());

    /**
     * 是否把产物（补货器：与模板不符的物品）收入维度网络
     */
    public static final RegistryObject<MachineSetting<ReceiveMode>> RECEIVE = SETTINGS.register("receive",
            () -> MachineSetting.enumeration(ReceiveMode.class, ReceiveMode.STOP)
                    .label("ui.beyonddimensions.furnace.receive")
                    .hint("ui.beyonddimensions.furnace.receive.hint")
                    .editor(SettingEditor.toggle(ReceiveMode.OPEN, ReceiveMode.STOP))
                    .legacyKey("receive_mode")
                    .legacyItemKey("receive_mode")
                    .build());

    /**
     * 是否均匀分配各列的原料
     */
    public static final RegistryObject<MachineSetting<AutoSortMode>> SORT = SETTINGS.register("sort",
            () -> MachineSetting.enumeration(AutoSortMode.class, AutoSortMode.STOP)
                    .label("ui.beyonddimensions.furnace.sorting")
                    .hint("ui.beyonddimensions.furnace.sorting.hint")
                    .editor(SettingEditor.toggle(AutoSortMode.OPEN, AutoSortMode.STOP))
                    .legacyKey("sort_mode")
                    .build());

    public static final RegistryObject<MachineSetting<HopperItemMode>> COLLECT_ITEMS = SETTINGS.register("collect_items",
            () -> MachineSetting.enumeration(HopperItemMode.class, HopperItemMode.ALLOW)
                    .label(MACHINE + "collect.items")
                    .editor(SettingEditor.chip(HopperItemMode.ALLOW, HopperItemMode.DENY, MACHINE + "collect.items.short"))
                    .legacyKey("hopper_item_model")
                    .legacyItemKey("hopper_item_mode")
                    .build());

    public static final RegistryObject<MachineSetting<HopperXpMode>> COLLECT_EXPERIENCE = SETTINGS.register("collect_experience",
            () -> MachineSetting.enumeration(HopperXpMode.class, HopperXpMode.DENY)
                    .label(MACHINE + "collect.experience")
                    .editor(SettingEditor.chip(HopperXpMode.ALLOW, HopperXpMode.DENY, MACHINE + "collect.experience.short"))
                    .legacyKey("hopper_xp_mode")
                    .legacyItemKey("hopper_xp_mode")
                    .build());

    public static final RegistryObject<MachineSetting<HopperFluidMode>> COLLECT_FLUIDS = SETTINGS.register("collect_fluids",
            () -> MachineSetting.enumeration(HopperFluidMode.class, HopperFluidMode.DENY)
                    .label(MACHINE + "collect.fluids")
                    .editor(SettingEditor.chip(HopperFluidMode.ALLOW, HopperFluidMode.DENY, MACHINE + "collect.fluids.short"))
                    .legacyKey("hopper_fluid_mode")
                    .legacyItemKey("hopper_fluid_mode")
                    .build());

    public static final RegistryObject<MachineSetting<HopperNBTMode>> COLLECT_COMPONENTS = SETTINGS.register("collect_components",
            () -> MachineSetting.enumeration(HopperNBTMode.class, HopperNBTMode.DENY)
                    .label(MACHINE + "collect.components")
                    .hint(MACHINE + "collect.components.hint")
                    .editor(SettingEditor.toggle(HopperNBTMode.ALLOW, HopperNBTMode.DENY))
                    .legacyKey("hopper_nbt_mode")
                    .legacyItemKey("hopper_nbt_mode")
                    .build());

    /**
     * 收集范围；各机器的实际范围不同，选项说明由机器类型给出
     */
    public static final RegistryObject<MachineSetting<HopperRangeMode>> RANGE = SETTINGS.register("range",
            () -> MachineSetting.enumeration(HopperRangeMode.class, HopperRangeMode.RADIUS_MID)
                    .label(MACHINE + "range")
                    .editor(SettingEditor.cycle(List.of(HopperRangeMode.values()), mode -> MACHINE + "range." + rangeName(mode)))
                    .legacyKey("hopper_range_mode")
                    .legacyItemKey("hopper_range_mode")
                    .build());

    /**
     * 喂食器何时喂食
     */
    public static final RegistryObject<MachineSetting<FeederMode>> FEEDING = SETTINGS.register("feeding",
            () -> MachineSetting.enumeration(FeederMode.class, FeederMode.NORMAL)
                    .label("ui.beyonddimensions.feeder.mode")
                    .editor(SettingEditor.cycle(List.of(FeederMode.values()), mode -> "ui.beyonddimensions.feeder.mode." + lowercase(mode))
                            .withHints(mode -> "ui.beyonddimensions.feeder.mode." + lowercase(mode) + ".hint"))
                    .legacyItemKey("feeder_mode")
                    .build());

    /**
     * 磁铁吸取的物品送到哪里；默认只送网络，旧磁铁的去向不变
     */
    public static final RegistryObject<MachineSetting<PickupTarget>> PICKUP_TARGET = SETTINGS.register("pickup_target",
            () -> MachineSetting.enumeration(PickupTarget.class, PickupTarget.NETWORK_ONLY)
                    .label(MACHINE + "pickup_target")
                    .editor(SettingEditor.cycle(List.of(PickupTarget.values()), target -> MACHINE + "pickup_target." + lowercase(target))
                            .withHints(target -> MACHINE + "pickup_target." + lowercase(target) + ".hint"))
                    .build());

    /**
     * 存量发信器的发信条件
     */
    public static final RegistryObject<MachineSetting<EmitMode>> EMIT = SETTINGS.register("emit",
            () -> MachineSetting.enumeration(EmitMode.class, EmitMode.AT_LEAST)
                    .label("ui.beyonddimensions.emitter.mode")
                    .editor(SettingEditor.buttons(List.of(EmitMode.values()), mode -> "ui.beyonddimensions.emitter.mode." + lowercase(mode)))
                    .build());

    /**
     * 相对正面的六个面各自的输入输出，见 {@link MachineSides}
     */
    private static final Map<RelativeSide, RegistryObject<MachineSetting<SideMode>>> SIDES = new EnumMap<>(RelativeSide.class);

    static
    {
        for (RelativeSide side : RelativeSide.values())
            SIDES.put(side, SETTINGS.register("side_" + lowercase(side), () -> sideSetting(side)));
    }

    private BDMachineSettings()
    {
    }

    /**
     * 相对正面的 side 那一面的输入输出。机器类型列出全部六个面时，设置页把它们合成一张面配置图，每一面是一个循环切换的格子
     */
    public static RegistryObject<MachineSetting<SideMode>> side(RelativeSide side)
    {
        return SIDES.get(side);
    }

    private static MachineSetting<SideMode> sideSetting(RelativeSide side)
    {
        return MachineSetting.enumeration(SideMode.class, SideMode.BOTH)
                .label(SIDE + lowercase(side))
                .editor(SettingEditor.cycle(List.of(SideMode.values()), mode -> SIDE + "mode." + lowercase(mode)))
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
