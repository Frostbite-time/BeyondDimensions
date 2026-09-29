package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdMeter
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.formatReadout
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.init.BDItems
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings
import com.wintercogs.beyonddimensions.common.machine.FilterMode
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode
import com.wintercogs.beyonddimensions.common.machine.HopperFluidMode
import com.wintercogs.beyonddimensions.common.machine.HopperItemMode
import com.wintercogs.beyonddimensions.common.machine.HopperNBTMode
import com.wintercogs.beyonddimensions.common.machine.HopperXpMode
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.machine.AutoSortMode
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import com.wintercogs.beyonddimensions.common.menu.NetFeederMenu
import com.wintercogs.beyonddimensions.common.menu.NetFurnaceMenu
import com.wintercogs.beyonddimensions.common.menu.NetHopperMenu
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu
import com.wintercogs.beyonddimensions.common.menu.NetRestockerMenu
import com.wintercogs.beyonddimensions.common.menu.XpExchangeMenu
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

// ---- 各机器共用的设置 ----

private fun redstone(current: () -> RedStoneControlMode, editable: () -> Boolean, request: (Int) -> Boolean) =
    ModeSetting(
        tr("ui.beyonddimensions.machine.redstone"),
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") },
        { current().ordinal },
        editable,
        request,
    )

private fun filter(current: () -> FilterMode, editable: () -> Boolean, request: (Int) -> Boolean) =
    ModeSetting(
        tr("ui.beyonddimensions.machine.filter"),
        FilterMode.entries.map { tr("ui.beyonddimensions.machine.filter.${it.name.lowercase()}") },
        { current().ordinal },
        editable,
        request,
    )

/** 弹出模式：开启时主动向相邻方块输出 */
private fun pop(description: String, current: () -> PopMode, editable: () -> Boolean, request: (Int) -> Boolean) =
    ToggleSetting(
        tr("ui.beyonddimensions.machine.pop"),
        description,
        { current() == PopMode.OPEN },
        editable,
        { request((if (it) PopMode.OPEN else PopMode.STOP).ordinal) },
    )

/** 随身设备的总开关：开启为“始终工作”，关闭为“停止” */
private fun working(current: () -> RedStoneControlMode, editable: () -> Boolean, request: (Int) -> Boolean) =
    ToggleSetting(
        tr("ui.beyonddimensions.machine.working"),
        null,
        { current() == RedStoneControlMode.IGNORE },
        editable,
        { request(if (it) 0 else 1) },
    )

/** 允许/拒绝两态的收集项 */
private fun allow(key: String, allowed: () -> Boolean, editable: () -> Boolean, request: (Int) -> Boolean) =
    ToggleSetting(tr("ui.beyonddimensions.machine.collect.$key"), null, allowed, editable, { request(if (it) 0 else 1) })

private val RANGE_KEYS = listOf("lowest", "low", "mid", "high", "highest", "chunk")

private fun range(device: String, current: () -> Int, editable: () -> Boolean, request: (Int) -> Boolean): ModeSetting {
    val descriptions = RANGE_KEYS.map { tr("ui.beyonddimensions.machine.range.$device.$it") }
    return ModeSetting(
        tr("ui.beyonddimensions.machine.range"),
        RANGE_KEYS.map { tr("ui.beyonddimensions.machine.range.$it") },
        current,
        editable,
        request,
        describe = { descriptions.getOrNull(it) },
        cycle = true,
    )
}

// ---- 维度网络能量通道 ----

data class EnergyReadout(val stored: Long, val capacity: Long, val rate: Long) : MachineReadout

class EnergyScreen private constructor(menu: NetEnergyMenu, title: Component, layout: MachineLayout, text: EnergyText) :
    MachineScreen<NetEnergyMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                pop(text.popHint, menu::output, menu::outputEditable, menu::requestOutput),
                redstone(menu::redstone, menu::redstoneEditable, menu::requestRedstone),
            ),
            { EnergyReadout(menu.stored(), menu.capacity(), menu.rate()) },
        ),
        layout,
        { state, _ -> EnergyContent(state.readout as? EnergyReadout, text) },
    ) {
    constructor(
        menu: NetEnergyMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDBlocks.NET_ENERGY_PATHWAY.get(), title.string), EnergyText())
}

class EnergyText {
    val network = tr("ui.beyonddimensions.energy.network")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val unit = "FE"
    val rateUnit = "FE/t"
}

@Composable
private fun EnergyContent(readout: EnergyReadout?, text: EnergyText) {
    val colors = Bd.colors
    val stored = readout?.stored ?: 0
    val capacity = readout?.capacity ?: 0
    val rate = readout?.rate ?: 0
    BdSectionLabel(text.network) { OreText(text.unit, color = colors.faint, style = Bd.caption) }
    Spacer(Modifier.height(5.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        OreText(formatReadout(stored), color = colors.text, style = Bd.title, maxLines = 1)
        Spacer(Modifier.width(3.dp))
        OreText("/ " + formatReadout(capacity), color = colors.muted, maxLines = 1)
        Spacer(Modifier.weight(1f))
        val ink = if (rate >= 0) colors.online else colors.danger
        Row(verticalAlignment = Alignment.CenterVertically) {
            OreIcon(if (rate >= 0) OreGlyph.ArrowUp else OreGlyph.ArrowDown, Modifier.size(6.dp), color = ink)
            Spacer(Modifier.width(2.dp))
            OreText(formatReadout(kotlin.math.abs(rate)) + " " + text.rateUnit, color = ink, style = Bd.caption, maxLines = 1)
        }
    }
    Spacer(Modifier.height(4.dp))
    BdMeter(if (capacity > 0) (stored.toDouble() / capacity).toFloat() else 0f, Modifier.fillMaxWidth().height(11.dp))
    Spacer(Modifier.height(2.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (tick in listOf("0", "25", "50", "75", "100%")) OreText(tick, color = colors.faint, style = Bd.caption, maxLines = 1)
    }
    Spacer(Modifier.height(8.dp))
}

// ---- 网络泵 ----

class PumpScreen private constructor(menu: NetPumpMenu, title: Component, layout: MachineLayout) :
    MachineScreen<NetPumpMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                filter(menu::filter, menu::filterEditable, menu::requestFilter),
                redstone(menu::redstone, menu::redstoneEditable, menu::requestRedstone),
            ),
        ),
        layout,
        { _, slots -> FlagSlots(layout.text.filters, layout.flagSlots, slots) },
    ) {
    constructor(
        menu: NetPumpMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDBlocks.NET_PUMP_BLOCK.get(), title.string))
}

// ---- 网络漏斗 ----

class HopperScreen private constructor(menu: NetHopperMenu, title: Component, layout: MachineLayout) :
    MachineScreen<NetHopperMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                filter(menu::filter, menu::filterEditable, menu::requestFilter),
                redstone(menu::redstone, menu::redstoneEditable, menu::requestRedstone),
                allow("items", { menu.items() == HopperItemMode.ALLOW }, menu::itemsEditable, menu::requestItems),
                allow("experience", { menu.experience() == HopperXpMode.ALLOW }, menu::experienceEditable, menu::requestExperience),
                allow("fluids", { menu.fluids() == HopperFluidMode.ALLOW }, menu::fluidsEditable, menu::requestFluids),
                allow("components", { menu.components() == HopperNBTMode.ALLOW }, menu::componentsEditable, menu::requestComponents),
                range("hopper", { menu.range().ordinal }, menu::rangeEditable, menu::requestRange),
            ),
        ),
        layout,
        { _, slots -> FlagSlots(layout.text.filters, layout.flagSlots, slots) },
    ) {
    constructor(
        menu: NetHopperMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDBlocks.NET_HOPPER_BLOCK.get(), title.string))
}

// ---- 网络磁铁 ----

class MagnetScreen private constructor(menu: NetMagnetMenu, title: Component, layout: MachineLayout) :
    MachineScreen<NetMagnetMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                working(menu::working, menu::workingEditable, menu::requestWorking),
                filter(menu::filter, menu::filterEditable, menu::requestFilter),
                allow("items", { menu.items() == HopperItemMode.ALLOW }, menu::itemsEditable, menu::requestItems),
                allow("experience", { menu.experience() == HopperXpMode.ALLOW }, menu::experienceEditable, menu::requestExperience),
                allow("fluids", { menu.fluids() == HopperFluidMode.ALLOW }, menu::fluidsEditable, menu::requestFluids),
                allow("components", { menu.components() == HopperNBTMode.ALLOW }, menu::componentsEditable, menu::requestComponents),
                range("magnet", { menu.range().ordinal }, menu::rangeEditable, menu::requestRange),
            ),
        ),
        layout,
        { _, slots -> FlagSlots(layout.text.filters, layout.flagSlots, slots) },
    ) {
    constructor(
        menu: NetMagnetMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDItems.NET_MAGNET_ITEM.get(), title.string))
}

// ---- 网络喂食器 ----

class FeederScreen private constructor(menu: NetFeederMenu, title: Component, layout: MachineLayout) :
    MachineScreen<NetFeederMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                working(menu::working, menu::workingEditable, menu::requestWorking),
                feeding(menu),
            ),
        ),
        layout,
        { _, slots -> FlagSlots(tr("ui.beyonddimensions.feeder.foods"), layout.flagSlots, slots) },
    ) {
    constructor(
        menu: NetFeederMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDItems.NET_FEEDER_ITEM.get(), title.string))

    private companion object {
        fun feeding(menu: NetFeederMenu): ModeSetting {
            val modes = com.wintercogs.beyonddimensions.common.machine.FeederMode.entries
            val descriptions = modes.map { tr("ui.beyonddimensions.feeder.mode.${it.name.lowercase()}.hint") }
            return ModeSetting(
                tr("ui.beyonddimensions.feeder.mode"),
                modes.map { tr("ui.beyonddimensions.feeder.mode.${it.name.lowercase()}") },
                { menu.feeding().ordinal },
                menu::feedingEditable,
                menu::requestFeeding,
                describe = { descriptions.getOrNull(it) },
                cycle = true,
            )
        }
    }
}

// ---- 网络补货器 ----

class RestockerScreen private constructor(menu: NetRestockerMenu, title: Component, layout: MachineLayout) :
    MachineScreen<NetRestockerMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                working(menu::working, menu::workingEditable, menu::requestWorking),
                ToggleSetting(
                    tr("ui.beyonddimensions.machine.fuzzy"),
                    tr("ui.beyonddimensions.machine.fuzzy.hint"),
                    { menu.matching() == FuzzyMode.ENABLE },
                    menu::matchingEditable,
                    { menu.requestMatching((if (it) FuzzyMode.ENABLE else FuzzyMode.DISABLE).ordinal) },
                ),
                ToggleSetting(
                    tr("ui.beyonddimensions.restocker.recycle"),
                    tr("ui.beyonddimensions.restocker.recycle.hint"),
                    { menu.recycle() == ReceiveMode.OPEN },
                    menu::recycleEditable,
                    { menu.requestRecycle((if (it) ReceiveMode.OPEN else ReceiveMode.STOP).ordinal) },
                ),
            ),
        ),
        layout,
        { _, slots -> RestockerTemplates(layout, slots) },
    ) {
    constructor(
        menu: NetRestockerMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDItems.NET_RESTOCKER_ITEM.get(), title.string))
}

// ---- 经验棒 ----

class XpExchangeScreen private constructor(menu: XpExchangeMenu, title: Component, layout: MachineLayout, usage: List<String>) :
    MachineScreen<XpExchangeMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                ToggleSetting(
                    tr("ui.beyonddimensions.xp.keep"),
                    tr("ui.beyonddimensions.xp.keep.hint"),
                    menu::keep,
                    menu::keepEditable,
                    menu::requestKeep,
                ),
                NumberSetting(
                    tr("ui.beyonddimensions.xp.target"),
                    tr("ui.beyonddimensions.xp.target.hint", XpExchangeSettings.MAX_TARGET_LEVEL),
                    menu::target,
                    0..XpExchangeSettings.MAX_TARGET_LEVEL,
                    menu::targetEditable,
                    menu::requestTarget,
                ),
            ),
        ),
        layout,
        { _, _ -> XpUsage(usage) },
    ) {
    constructor(
        menu: XpExchangeMenu,
        inventory: Inventory,
        title: Component,
    ) : this(
        menu,
        title,
        MachineLayout(menu, BDItems.XP_EXCHANGE_ITEM.get(), title.string),
        tr("tooltip.beyonddimensions.item.xp_exchange").split('\n'),
    )
}

@Composable
private fun XpUsage(lines: List<String>) {
    val colors = Bd.colors
    BdSectionLabel(tr("ui.beyonddimensions.xp.usage"))
    Spacer(Modifier.height(4.dp))
    Column(Modifier.fillMaxWidth().background(colors.surface).padding(5.dp)) {
        for (line in lines) OreText(line, color = colors.muted, style = Bd.caption)
    }
    Spacer(Modifier.height(7.dp))
}

// ---- 网络熔炉（含高炉、烟熏炉） ----

data class LaneView(val cooking: Float, val burning: Float)

data class FurnaceReadout(val lanes: List<LaneView>) : MachineReadout

class FurnaceScreen private constructor(menu: NetFurnaceMenu, title: Component, layout: MachineLayout, groups: FurnaceGroups) :
    MachineScreen<NetFurnaceMenu>(
        menu,
        title,
        MachineController(
            menu::ready,
            listOf(
                pop(tr("ui.beyonddimensions.furnace.pop.hint"), menu::output, menu::outputEditable, menu::requestOutput),
                ToggleSetting(
                    tr("ui.beyonddimensions.furnace.receive"),
                    tr("ui.beyonddimensions.furnace.receive.hint"),
                    { menu.receive() == ReceiveMode.OPEN },
                    menu::receiveEditable,
                    { menu.requestReceive((if (it) ReceiveMode.OPEN else ReceiveMode.STOP).ordinal) },
                ),
                ToggleSetting(
                    tr("ui.beyonddimensions.furnace.sorting"),
                    tr("ui.beyonddimensions.furnace.sorting.hint"),
                    { menu.sorting() == AutoSortMode.OPEN },
                    menu::sortingEditable,
                    { menu.requestSorting((if (it) AutoSortMode.OPEN else AutoSortMode.STOP).ordinal) },
                ),
                redstone(menu::redstone, menu::redstoneEditable, menu::requestRedstone),
            ),
            { FurnaceReadout(menu.lanes().map { LaneView(fraction(it.cooking(), it.cookingTotal()), fraction(it.burning(), it.burningTotal())) }) },
        ),
        layout,
        { state, slots -> FurnaceContent(state.readout as? FurnaceReadout, groups, slots) },
    ) {
    constructor(
        menu: NetFurnaceMenu,
        inventory: Inventory,
        title: Component,
    ) : this(
        menu,
        title,
        MachineLayout(menu, menu.be?.blockState?.block ?: BDBlocks.NET_FURNACE_BLOCK.get(), title.string),
        FurnaceGroups(menu),
    )

    private companion object {
        fun fraction(value: Int, total: Int) = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f
    }
}

class FurnaceGroups(menu: NetFurnaceMenu) {
    val inputFilters: List<Int> = menu.inputFilterSlotIds()
    val fuelFilters: List<Int> = menu.fuelFilterSlotIds()
    val inputs: List<Int> = menu.inputStorageSlotIds()
    val fuel: List<Int> = menu.fuelStorageSlotIds() + menu.fuelReturnSlotIds()
    val outputs: List<Int> = menu.outputStorageSlotIds()
    val filters = tr("ui.beyonddimensions.furnace.filters")
    val input = tr("ui.beyonddimensions.furnace.input")
    val fuelLabel = tr("ui.beyonddimensions.furnace.fuel")
    val smelting = tr("ui.beyonddimensions.furnace.smelting")
    val output = tr("ui.beyonddimensions.furnace.output")
}

@Composable
private fun <M : com.wintercogs.beyonddimensions.common.menu.BDBaseMenu> FurnaceContent(
    readout: FurnaceReadout?,
    groups: FurnaceGroups,
    slots: dev.compixel.forge.slots.ComposeMenuSlots<M>,
) {
    val colors = Bd.colors
    BdSectionLabel(groups.filters)
    Spacer(Modifier.height(4.dp))
    FilterRow(groups.input, groups.inputFilters, slots)
    Spacer(Modifier.height(2.dp))
    FilterRow(groups.fuelLabel, groups.fuelFilters, slots)
    Spacer(Modifier.height(7.dp))
    BdSectionLabel(groups.smelting)
    Spacer(Modifier.height(4.dp))
    Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, groups.inputs, 9) }
    // 每一列一条熔炼进度与燃烧余量
    Row(Modifier.padding(start = 0.5.dp, top = 2.dp, bottom = 2.dp)) {
        val lanes = readout?.lanes.orEmpty()
        repeat(9) { lane ->
            val view = lanes.getOrNull(lane)
            Column(Modifier.width(18.dp).padding(horizontal = 2.dp)) {
                LaneBar(view?.cooking ?: 0f, colors.accent)
                Spacer(Modifier.height(1.dp))
                LaneBar(view?.burning ?: 0f, colors.warning)
            }
        }
    }
    Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, groups.outputs, 9) }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OreText(groups.fuelLabel, Modifier.width(36.dp), color = colors.muted, style = Bd.caption, maxLines = 1)
        Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, groups.fuel, groups.fuel.size.coerceAtLeast(1)) }
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun <M : com.wintercogs.beyonddimensions.common.menu.BDBaseMenu> FilterRow(
    label: String,
    ids: List<Int>,
    slots: dev.compixel.forge.slots.ComposeMenuSlots<M>,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OreText(label, Modifier.width(18.dp), color = Bd.colors.muted, style = Bd.caption, maxLines = 1)
        Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids, ids.size.coerceAtLeast(1)) }
    }
}

@Composable
private fun LaneBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(2.dp).background(Bd.colors.sunken)) {
        if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(color))
    }
}

/**
 * 补货模板与玩家背包一一对应：模板 0–8 是快捷栏，9–35 是主背包，36–40 是盔甲与副手。
 * 按背包的排布显示，一眼就能看出每个模板对应哪一格。
 */
@Composable
private fun RestockerTemplates(layout: MachineLayout, slots: dev.compixel.forge.slots.ComposeMenuSlots<NetRestockerMenu>) {
    val flags = layout.flagSlots
    BdSectionLabel(tr("ui.beyonddimensions.restocker.templates"))
    Spacer(Modifier.height(4.dp))
    Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) {
        com.wintercogs.beyonddimensions.client.ui.base.BdPlayerInventory(slots, flags.subList(9, 36) + flags.subList(0, 9))
    }
    Spacer(Modifier.height(7.dp))
    FlagSlots(tr("ui.beyonddimensions.restocker.equipment"), flags.drop(36), slots)
}
