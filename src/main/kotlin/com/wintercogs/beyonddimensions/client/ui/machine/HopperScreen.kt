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
import com.wintercogs.beyonddimensions.common.machine.*
import com.wintercogs.beyonddimensions.common.menu.NetHopperMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络漏斗：主页面是过滤槽与玩家背包；设置页是过滤、红石、收集的种类与范围 */
class HopperScreen(menu: NetHopperMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetHopperMenu, HopperState, HopperAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_HOPPER_BLOCK.get()))
    private val filterSlots = menu.flagSlotIds()
    private val playerSlots = menu.playerSlotIds()
    private val text = HopperText(title.string)

    override fun snapshot() =
        HopperState(
            filter = container.filter(),
            filterEditable = container.filterEditable(),
            redstone = container.redstone(),
            redstoneEditable = container.redstoneEditable(),
            items = container.items() == HopperItemMode.ALLOW,
            itemsEditable = container.itemsEditable(),
            experience = container.experience() == HopperXpMode.ALLOW,
            experienceEditable = container.experienceEditable(),
            fluids = container.fluids() == HopperFluidMode.ALLOW,
            fluidsEditable = container.fluidsEditable(),
            components = container.components() == HopperNBTMode.ALLOW,
            componentsEditable = container.componentsEditable(),
            range = container.range().ordinal,
            rangeEditable = container.rangeEditable(),
        )

    override fun handle(action: HopperAction) {
        // 收集项的第一个选项是“允许”，第二个是“拒绝”
        fun choice(allowed: Boolean) = if (allowed) 0 else 1
        when (action) {
            is HopperAction.SetFilter -> container.requestFilter(action.mode.ordinal)
            is HopperAction.SetRedstone -> container.requestRedstone(action.mode.ordinal)
            is HopperAction.SetItems -> container.requestItems(choice(action.allowed))
            is HopperAction.SetExperience -> container.requestExperience(choice(action.allowed))
            is HopperAction.SetFluids -> container.requestFluids(choice(action.allowed))
            is HopperAction.SetComponents -> container.requestComponents(choice(action.allowed))
            is HopperAction.SetRange -> container.requestRange(action.range)
        }
    }

    @Composable
    override fun Content(state: HopperState, slots: ComposeMenuSlots<NetHopperMenu>) {
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
                BdTabPage(settingsOpen) { HopperSettings(state, settingsOpen, text, ::send) }
            }
        }
    }
}

/** 设置页的全部设置；[shown] 为 false 时设置页正在淡出，控件不再响应 */
@Composable
private fun HopperSettings(state: HopperState, shown: Boolean, text: HopperText, send: (HopperAction) -> Boolean) {
    BdSectionLabel(text.settings)
    BdModeSetting(text.filter, text.filterOptions, state.filter.ordinal, shown && state.filterEditable) {
        send(HopperAction.SetFilter(FilterMode.entries[it]))
    }
    BdModeSetting(text.redstone, text.redstoneOptions, state.redstone.ordinal, shown && state.redstoneEditable) {
        send(HopperAction.SetRedstone(RedStoneControlMode.entries[it]))
    }
    BdSettingRow(text.items) {
        BdToggle(
            state.items,
            { send(HopperAction.SetItems(it)) },
            shown && state.itemsEditable
        )
    }
    BdSettingRow(text.experience) {
        BdToggle(state.experience, { send(HopperAction.SetExperience(it)) }, shown && state.experienceEditable)
    }
    BdSettingRow(text.fluids) {
        BdToggle(
            state.fluids,
            { send(HopperAction.SetFluids(it)) },
            shown && state.fluidsEditable
        )
    }
    BdSettingRow(text.components) {
        BdToggle(state.components, { send(HopperAction.SetComponents(it)) }, shown && state.componentsEditable)
    }
    BdModeSetting(
        text.range,
        text.rangeOptions,
        state.range,
        shown && state.rangeEditable,
        description = text.rangeHints[state.range],
        cycle = true,
    ) {
        send(HopperAction.SetRange(it))
    }
}

data class HopperState(
    val filter: FilterMode,
    val filterEditable: Boolean,
    val redstone: RedStoneControlMode,
    val redstoneEditable: Boolean,
    val items: Boolean,
    val itemsEditable: Boolean,
    val experience: Boolean,
    val experienceEditable: Boolean,
    val fluids: Boolean,
    val fluidsEditable: Boolean,
    val components: Boolean,
    val componentsEditable: Boolean,
    val range: Int,
    val rangeEditable: Boolean,
)

sealed interface HopperAction {
    data class SetFilter(val mode: FilterMode) : HopperAction

    data class SetRedstone(val mode: RedStoneControlMode) : HopperAction

    data class SetItems(val allowed: Boolean) : HopperAction

    data class SetExperience(val allowed: Boolean) : HopperAction

    data class SetFluids(val allowed: Boolean) : HopperAction

    data class SetComponents(val allowed: Boolean) : HopperAction

    data class SetRange(val range: Int) : HopperAction
}

private val RANGES = listOf("lowest", "low", "mid", "high", "highest", "chunk")

private class HopperText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
    val filter = tr("ui.beyonddimensions.machine.filter")
    val filterOptions = FilterMode.entries.map { tr("ui.beyonddimensions.machine.filter.${it.name.lowercase()}") }
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
    val items = tr("ui.beyonddimensions.machine.collect.items")
    val experience = tr("ui.beyonddimensions.machine.collect.experience")
    val fluids = tr("ui.beyonddimensions.machine.collect.fluids")
    val components = tr("ui.beyonddimensions.machine.collect.components")
    val range = tr("ui.beyonddimensions.machine.range")
    val rangeOptions = RANGES.map { tr("ui.beyonddimensions.machine.range.$it") }
    val rangeHints = RANGES.map { tr("ui.beyonddimensions.machine.range.hopper.$it") }
}
