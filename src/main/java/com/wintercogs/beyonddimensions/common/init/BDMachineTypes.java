package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.machine.SettingEditor;
import com.wintercogs.beyonddimensions.api.registry.BDRegistries;
import com.wintercogs.beyonddimensions.common.machine.HopperRangeMode;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * BD 的机器类型；设置页按这里列出的顺序生成
 */
public final class BDMachineTypes
{
    public static final DeferredRegister<MachineType> TYPES = DeferredRegister.create(BDRegistries.MACHINE_TYPE_KEY, BDConstants.MODID);

    public static final DeferredHolder<MachineType, MachineType> NET_INTERFACE = TYPES.register("net_interface",
            () -> MachineType.builder(BDBlocks.NET_INTERFACE)
                    .setting(BDMachineSettings.POP, entry -> entry.hint("ui.beyonddimensions.interface.pop.hint")
                            .lockedHint("tooltip.button.beyonddimensions.popmode_mounted_unavailable"))
                    .setting(BDMachineSettings.REDSTONE)
                    .setting(BDMachineSettings.FUZZY)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_HOPPER = TYPES.register("net_hopper",
            () -> MachineType.builder(BDBlocks.NET_HOPPER_BLOCK)
                    .setting(BDMachineSettings.FILTER)
                    .setting(BDMachineSettings.REDSTONE)
                    .setting(BDMachineSettings.COLLECT_ITEMS)
                    .setting(BDMachineSettings.COLLECT_EXPERIENCE)
                    .setting(BDMachineSettings.COLLECT_FLUIDS)
                    .setting(BDMachineSettings.COLLECT_COMPONENTS)
                    .setting(BDMachineSettings.RANGE, entry -> entry.editor(rangeHints("hopper")))
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_PUMP = TYPES.register("net_pump",
            () -> MachineType.builder(BDBlocks.NET_PUMP_BLOCK)
                    .setting(BDMachineSettings.FILTER)
                    .setting(BDMachineSettings.REDSTONE)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_ENERGY_PATHWAY = TYPES.register("net_energy_pathway",
            () -> MachineType.builder(BDBlocks.NET_ENERGY_PATHWAY)
                    .setting(BDMachineSettings.POP, entry -> entry.hint("ui.beyonddimensions.energy.pop.hint"))
                    .setting(BDMachineSettings.REDSTONE)
                    .build());

    public static final DeferredHolder<MachineType, MachineType> NET_FURNACE = TYPES.register("net_furnace",
            () -> furnace(BDBlocks.NET_FURNACE_BLOCK));

    public static final DeferredHolder<MachineType, MachineType> NET_BLAST_FURNACE = TYPES.register("net_blast_furnace",
            () -> furnace(BDBlocks.NET_BLAST_FURNACE_BLOCK));

    public static final DeferredHolder<MachineType, MachineType> NET_SMOKER = TYPES.register("net_smoker",
            () -> furnace(BDBlocks.NET_SMOKER_BLOCK));

    public static final DeferredHolder<MachineType, MachineType> NET_MAGNET = TYPES.register("net_magnet",
            () -> MachineType.builder(BDItems.NET_MAGNET_ITEM)
                    .setting(BDMachineSettings.WORKING)
                    .setting(BDMachineSettings.FILTER)
                    .setting(BDMachineSettings.COLLECT_ITEMS)
                    .setting(BDMachineSettings.COLLECT_EXPERIENCE)
                    .setting(BDMachineSettings.COLLECT_FLUIDS)
                    .setting(BDMachineSettings.COLLECT_COMPONENTS)
                    .setting(BDMachineSettings.PICKUP_TARGET)
                    .setting(BDMachineSettings.RANGE, entry -> entry.editor(rangeHints("magnet")))
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
        return MachineType.builder(block)
                .setting(BDMachineSettings.POP, entry -> entry.hint("ui.beyonddimensions.furnace.pop.hint"))
                .setting(BDMachineSettings.RECEIVE)
                .setting(BDMachineSettings.SORT)
                .setting(BDMachineSettings.REDSTONE)
                .build();
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
