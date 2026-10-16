package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.machine.SettingEditor;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.machine.RelativeSide;
import com.wintercogs.beyonddimensions.common.machine.setting.HopperRangeMode;
import com.wintercogs.beyonddimensions.common.machine.setting.SideMode;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * BD 的机器类型；设置页按这里列出的顺序生成
 */
public final class BDMachineTypes
{
    public static final DeferredRegister<MachineType> TYPES = DeferredRegister.create(BDRegistries.MACHINE_TYPE_KEY, BDConstants.MODID);

    public static final DeferredHolder<MachineType, MachineType> NET_INTERFACE = TYPES.register("net_interface",
            () -> withSides(MachineType.builder(BDBlocks.NET_INTERFACE), "ui.beyonddimensions.interface.pop.hint")
                    .setting(BDMachineSettings.REDSTONE)
                    .setting(BDMachineSettings.FUZZY)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_HOPPER = TYPES.register("net_hopper",
            () -> MachineType.builder(BDBlocks.NET_HOPPER_BLOCK)
                    .setting(BDMachineSettings.FILTER)
                    .setting(BDMachineSettings.FILTER_FUZZY)
                    .setting(BDMachineSettings.REDSTONE)
                    .setting(BDMachineSettings.COLLECT_ITEMS)
                    .setting(BDMachineSettings.COLLECT_EXPERIENCE)
                    .setting(BDMachineSettings.COLLECT_FLUIDS)
                    .setting(BDMachineSettings.COLLECT_COMPONENTS)
                    .setting(BDMachineSettings.RANGE, entry -> entry.editor(rangeHints("hopper")))
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_PUMP = TYPES.register("net_pump",
            () -> withFaces(MachineType.builder(BDBlocks.NET_PUMP_BLOCK), BDMachineTypes::pumpFace)
                    .setting(BDMachineSettings.FILTER)
                    .setting(BDMachineSettings.FILTER_FUZZY)
                    .setting(BDMachineSettings.REDSTONE)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_ENERGY_PATHWAY = TYPES.register("net_energy_pathway",
            () -> withSides(MachineType.builder(BDBlocks.NET_ENERGY_PATHWAY), "ui.beyonddimensions.energy.pop.hint")
                    .setting(BDMachineSettings.REDSTONE)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_FURNACE = TYPES.register("net_furnace",
            () -> furnace(BDBlocks.NET_FURNACE_BLOCK));

    public static final DeferredHolder<MachineType, MachineType> NET_BLAST_FURNACE = TYPES.register("net_blast_furnace",
            () -> furnace(BDBlocks.NET_BLAST_FURNACE_BLOCK));

    public static final DeferredHolder<MachineType, MachineType> NET_SMOKER = TYPES.register("net_smoker",
            () -> furnace(BDBlocks.NET_SMOKER_BLOCK));

    public static final DeferredHolder<MachineType, MachineType> NET_STOCK_EMITTER = TYPES.register("net_stock_emitter",
            () -> MachineType.builder(BDBlocks.NET_STOCK_EMITTER)
                    .setting(BDMachineSettings.EMIT)
                    .setting(BDMachineSettings.FUZZY)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_MAGNET = TYPES.register("net_magnet",
            () -> MachineType.builder(BDItems.NET_MAGNET_ITEM)
                    .section("ui.beyonddimensions.magnet.section.collect")
                    .setting(BDMachineSettings.WORKING)
                    .setting(BDMachineSettings.COLLECT_ITEMS, entry -> entry.chip("ui.beyonddimensions.magnet.collect.items"))
                    .setting(BDMachineSettings.COLLECT_EXPERIENCE, entry -> entry.chip("ui.beyonddimensions.magnet.collect.experience"))
                    .setting(BDMachineSettings.COLLECT_FLUIDS, entry -> entry.chip("ui.beyonddimensions.magnet.collect.fluids"))
                    .setting(BDMachineSettings.RANGE, entry -> entry.editor(rangeHints("magnet")))
                    .section("ui.beyonddimensions.magnet.section.filter")
                    .setting(BDMachineSettings.FILTER)
                    .setting(BDMachineSettings.FILTER_FUZZY)
                    .setting(BDMachineSettings.COLLECT_COMPONENTS, entry -> entry.label("ui.beyonddimensions.magnet.components")
                            .hint("ui.beyonddimensions.magnet.components.hint"))
                    .section("ui.beyonddimensions.magnet.section.destination")
                    .setting(BDMachineSettings.PICKUP_TARGET)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_FEEDER = TYPES.register("net_feeder",
            () -> MachineType.builder(BDItems.NET_FEEDER_ITEM)
                    .setting(BDMachineSettings.WORKING)
                    .setting(BDMachineSettings.FEEDING)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_RESTOCKER = TYPES.register("net_restocker",
            () -> MachineType.builder(BDItems.NET_RESTOCKER_ITEM)
                    .setting(BDMachineSettings.WORKING)
                    .setting(BDMachineSettings.FUZZY)
                    .setting(BDMachineSettings.RECEIVE, entry -> entry.label("ui.beyonddimensions.restocker.recycle")
                            .hint("ui.beyonddimensions.restocker.recycle.hint"))
                    .build());

    private BDMachineTypes()
    {
    }

    private static MachineType furnace(Supplier<? extends ItemLike> block)
    {
        return withSides(MachineType.builder(block), "ui.beyonddimensions.furnace.pop.hint")
                .setting(BDMachineSettings.RECEIVE)
                .setting(BDMachineSettings.SORT)
                .setting(BDMachineSettings.REDSTONE)
                .build();
    }

    // 六个面的输入输出；各机器都最先声明它们，面配置排在设置页最上面
    private static MachineType.Builder withFaces(MachineType.Builder builder, UnaryOperator<MachineType.EntryBuilder<SideMode>> face)
    {
        for (RelativeSide side : RelativeSide.values())
            builder.setting(BDMachineSettings.side(side), face);
        return builder;
    }

    // 六个面与主动弹出；popHint 说明这类机器弹出什么
    private static MachineType.Builder withSides(MachineType.Builder builder, String popHint)
    {
        return withFaces(builder, UnaryOperator.identity()).setting(BDMachineSettings.POP, entry -> entry.hint(popHint));
    }

    // 网络泵只从输入的面抽取，每一面只能输入或禁用，默认六面都抽取
    private static MachineType.EntryBuilder<SideMode> pumpFace(MachineType.EntryBuilder<SideMode> entry)
    {
        return entry.defaultValue(SideMode.INPUT).editor(base -> {
            var choice = (SettingEditor.Choice<SideMode>) base;
            return new SettingEditor.Choice<>(List.of(SideMode.INPUT, SideMode.NONE), choice.optionLabel(), choice.optionHint(), choice.cycle());
        });
    }

    // 各机器的实际收集范围不同，选项说明按机器给出
    private static UnaryOperator<SettingEditor<HopperRangeMode>> rangeHints(String machine)
    {
        return base -> ((SettingEditor.Choice<HopperRangeMode>) base)
                .withHints(mode -> "ui.beyonddimensions.machine.range." + machine + "." + BDMachineSettings.rangeName(mode));
    }

    public static void register(IEventBus modBus)
    {
        TYPES.register(modBus);
    }
}
