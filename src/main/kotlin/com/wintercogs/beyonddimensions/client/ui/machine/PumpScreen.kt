package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.FilterMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络泵：过滤槽与玩家背包；设置页是过滤模式与红石控制 */
class PumpScreen(menu: NetPumpMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetPumpMenu, PumpState, PumpAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_PUMP_BLOCK.get()))
    private val filterSlots = menu.flagSlotIds()
    private val playerSlots = menu.playerSlotIds()
    private val text = PumpText(title.string)

    override fun snapshot() =
        PumpState(
            filter = container.filter(),
            filterEditable = container.filterEditable(),
            redstone = container.redstone(),
            redstoneEditable = container.redstoneEditable(),
        )

    override fun handle(action: PumpAction) {
        when (action) {
            is PumpAction.SetFilter -> container.requestFilter(action.mode.ordinal)
            is PumpAction.SetRedstone -> container.requestRedstone(action.mode.ordinal)
        }
    }

    @Composable
    override fun Content(state: PumpState, slots: ComposeMenuSlots<NetPumpMenu>) {
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
                    BdSlotSection(text.filters, filterSlots, slots)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdModeSetting(
                        text.filter,
                        text.filterOptions,
                        state.filter.ordinal,
                        settingsOpen && state.filterEditable
                    ) {
                        send(PumpAction.SetFilter(FilterMode.entries[it]))
                    }
                    BdModeSetting(
                        text.redstone,
                        text.redstoneOptions,
                        state.redstone.ordinal,
                        settingsOpen && state.redstoneEditable
                    ) {
                        send(PumpAction.SetRedstone(RedStoneControlMode.entries[it]))
                    }
                }
            }
        }
    }
}

data class PumpState(
    val filter: FilterMode,
    val filterEditable: Boolean,
    val redstone: RedStoneControlMode,
    val redstoneEditable: Boolean,
)

sealed interface PumpAction {
    data class SetFilter(val mode: FilterMode) : PumpAction

    data class SetRedstone(val mode: RedStoneControlMode) : PumpAction
}

private class PumpText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
    val filter = tr("ui.beyonddimensions.machine.filter")
    val filterOptions = FilterMode.entries.map { tr("ui.beyonddimensions.machine.filter.${it.name.lowercase()}") }
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}
