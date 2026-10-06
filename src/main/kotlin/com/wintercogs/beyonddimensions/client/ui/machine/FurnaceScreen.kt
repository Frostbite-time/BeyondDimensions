package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdInventorySection
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_WINDOW_WIDTH
import com.wintercogs.beyonddimensions.client.ui.base.playerSlotIds
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphs
import com.wintercogs.beyonddimensions.client.ui.kit.BdHeader
import com.wintercogs.beyonddimensions.client.ui.kit.BdMainPage
import com.wintercogs.beyonddimensions.client.ui.kit.BdModeSetting
import com.wintercogs.beyonddimensions.client.ui.kit.BdRailTab
import com.wintercogs.beyonddimensions.client.ui.kit.BdScreenFrame
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.BdSettingRow
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabPage
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabbedWindow
import com.wintercogs.beyonddimensions.client.ui.kit.BdToggle
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.AutoSortMode
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetFurnaceMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/**
 * 网络熔炉（也用于高炉与烟熏炉）：输入与燃料的过滤、九列熔炼位与每列的进度、产物与燃料储备，以及玩家背包；
 * 设置页是弹出产物、接收输入、自动整理与红石控制。
 */
class FurnaceScreen(menu: NetFurnaceMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetFurnaceMenu, FurnaceState, FurnaceAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(menu.be?.blockState?.block ?: BDBlocks.NET_FURNACE_BLOCK.get()))
    private val slotIds = FurnaceSlots(menu)
    private val playerSlots = menu.playerSlotIds()
    private val text = FurnaceText(title.string)

    override fun snapshot() =
        FurnaceState(
            lanes = container.lanes().map { Lane(fraction(it.cooking(), it.cookingTotal()), fraction(it.burning(), it.burningTotal())) },
            pop = container.output() == PopMode.OPEN,
            popEditable = container.outputEditable(),
            receive = container.receive() == ReceiveMode.OPEN,
            receiveEditable = container.receiveEditable(),
            sorting = container.sorting() == AutoSortMode.OPEN,
            sortingEditable = container.sortingEditable(),
            redstone = container.redstone(),
            redstoneEditable = container.redstoneEditable(),
        )

    private fun fraction(value: Int, total: Int) = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f

    override fun handle(action: FurnaceAction) {
        when (action) {
            is FurnaceAction.SetPop -> container.requestOutput((if (action.on) PopMode.OPEN else PopMode.STOP).ordinal)
            is FurnaceAction.SetReceive -> container.requestReceive((if (action.on) ReceiveMode.OPEN else ReceiveMode.STOP).ordinal)
            is FurnaceAction.SetSorting -> container.requestSorting((if (action.on) AutoSortMode.OPEN else AutoSortMode.STOP).ordinal)
            is FurnaceAction.SetRedstone -> container.requestRedstone(action.mode.ordinal)
        }
    }

    @Composable
    override fun Content(state: FurnaceState, slots: ComposeMenuSlots<NetFurnaceMenu>) {
        var settingsOpen by remember { mutableStateOf(false) }
        slots.Interaction(enabled = !settingsOpen)
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                rail = {
                    BdRailTab(text.title, BdGlyphs.Main, selected = !settingsOpen) { settingsOpen = false }
                    Spacer(Modifier.weight(1f))
                    BdRailTab(text.settings, OreGlyph.Gear.art, selected = settingsOpen) { settingsOpen = true }
                },
            ) {
                BdMainPage(!settingsOpen) {
                    Furnace(state, slotIds, text, slots)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.pop, text.popHint) {
                        BdToggle(state.pop, { send(FurnaceAction.SetPop(it)) }, settingsOpen && state.popEditable)
                    }
                    BdSettingRow(text.receive, text.receiveHint) {
                        BdToggle(state.receive, { send(FurnaceAction.SetReceive(it)) }, settingsOpen && state.receiveEditable)
                    }
                    BdSettingRow(text.sorting, text.sortingHint) {
                        BdToggle(state.sorting, { send(FurnaceAction.SetSorting(it)) }, settingsOpen && state.sortingEditable)
                    }
                    BdModeSetting(text.redstone, text.redstoneOptions, state.redstone.ordinal, settingsOpen && state.redstoneEditable) {
                        send(FurnaceAction.SetRedstone(RedStoneControlMode.entries[it]))
                    }
                }
            }
        }
    }
}

/** 一列熔炼位的熔炼进度与燃料余量，都在 0 到 1 之间 */
data class Lane(val cooking: Float, val burning: Float)

data class FurnaceState(
    val lanes: List<Lane>,
    val pop: Boolean,
    val popEditable: Boolean,
    val receive: Boolean,
    val receiveEditable: Boolean,
    val sorting: Boolean,
    val sortingEditable: Boolean,
    val redstone: RedStoneControlMode,
    val redstoneEditable: Boolean,
)

sealed interface FurnaceAction {
    data class SetPop(val on: Boolean) : FurnaceAction

    data class SetReceive(val on: Boolean) : FurnaceAction

    data class SetSorting(val on: Boolean) : FurnaceAction

    data class SetRedstone(val mode: RedStoneControlMode) : FurnaceAction
}

/** 熔炉各区的槽位编号 */
private class FurnaceSlots(menu: NetFurnaceMenu) {
    val inputFilters: List<Int> = menu.inputFilterSlotIds()
    val fuelFilters: List<Int> = menu.fuelFilterSlotIds()
    val inputs: List<Int> = menu.inputStorageSlotIds()
    val fuel: List<Int> = menu.fuelStorageSlotIds() + menu.fuelReturnSlotIds()
    val outputs: List<Int> = menu.outputStorageSlotIds()
}

private class FurnaceText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.furnace.filters")
    val input = tr("ui.beyonddimensions.furnace.input")
    val fuel = tr("ui.beyonddimensions.furnace.fuel")
    val smelting = tr("ui.beyonddimensions.furnace.smelting")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.furnace.pop.hint")
    val receive = tr("ui.beyonddimensions.furnace.receive")
    val receiveHint = tr("ui.beyonddimensions.furnace.receive.hint")
    val sorting = tr("ui.beyonddimensions.furnace.sorting")
    val sortingHint = tr("ui.beyonddimensions.furnace.sorting.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions = RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}

@Composable
private fun Furnace(state: FurnaceState, ids: FurnaceSlots, text: FurnaceText, slots: ComposeMenuSlots<NetFurnaceMenu>) {
    val colors = Bd.colors
    BdSectionLabel(text.filters)
    Spacer(Modifier.height(4.dp))
    FilterRow(text.input, ids.inputFilters, slots)
    Spacer(Modifier.height(2.dp))
    FilterRow(text.fuel, ids.fuelFilters, slots)
    Spacer(Modifier.height(7.dp))
    BdSectionLabel(text.smelting)
    Spacer(Modifier.height(4.dp))
    Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids.inputs, 9) }
    // 每一列一条熔炼进度与燃料余量
    Row(Modifier.padding(start = 0.5.dp, top = 2.dp, bottom = 2.dp)) {
        for (lane in state.lanes) {
            Column(Modifier.width(18.dp).padding(horizontal = 2.dp)) {
                LaneBar(lane.cooking, colors.accent)
                Spacer(Modifier.height(1.dp))
                LaneBar(lane.burning, colors.warning)
            }
        }
    }
    Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids.outputs, 9) }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OreText(text.fuel, Modifier.width(36.dp), color = colors.muted, style = Bd.caption, maxLines = 1)
        Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids.fuel, ids.fuel.size) }
    }
}

/** 一行过滤槽，左侧是标签 */
@Composable
private fun FilterRow(label: String, ids: List<Int>, slots: ComposeMenuSlots<NetFurnaceMenu>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OreText(label, Modifier.width(18.dp), color = Bd.colors.muted, style = Bd.caption, maxLines = 1)
        Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids, ids.size) }
    }
}

@Composable
private fun LaneBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(2.dp).background(Bd.colors.sunken)) {
        if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).height(2.dp).background(color))
    }
}
