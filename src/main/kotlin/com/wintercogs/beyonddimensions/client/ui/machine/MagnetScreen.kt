package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdInventorySection
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotSection
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_WINDOW_WIDTH
import com.wintercogs.beyonddimensions.client.ui.base.flagSlotIds
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
import com.wintercogs.beyonddimensions.common.init.BDItems
import com.wintercogs.beyonddimensions.common.machine.FilterMode
import com.wintercogs.beyonddimensions.common.machine.HopperFluidMode
import com.wintercogs.beyonddimensions.common.machine.HopperItemMode
import com.wintercogs.beyonddimensions.common.machine.HopperNBTMode
import com.wintercogs.beyonddimensions.common.machine.HopperXpMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络磁铁：过滤槽与玩家背包；设置页是总开关、过滤模式、四类收集与范围 */
class MagnetScreen(menu: NetMagnetMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetMagnetMenu, MagnetState, MagnetAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDItems.NET_MAGNET_ITEM.get()))
    private val filterSlots = menu.flagSlotIds()
    private val playerSlots = menu.playerSlotIds()
    private val text = MagnetText(title.string)

    override fun snapshot() =
        MagnetState(
            working = container.working() == RedStoneControlMode.IGNORE,
            workingEditable = container.workingEditable(),
            filter = container.filter(),
            filterEditable = container.filterEditable(),
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

    override fun handle(action: MagnetAction) {
        // 总开关与收集项的第一个选项是“工作”“允许”，第二个是“停止”“拒绝”
        fun choice(on: Boolean) = if (on) 0 else 1
        when (action) {
            is MagnetAction.SetWorking -> container.requestWorking(choice(action.on))
            is MagnetAction.SetFilter -> container.requestFilter(action.mode.ordinal)
            is MagnetAction.SetItems -> container.requestItems(choice(action.allowed))
            is MagnetAction.SetExperience -> container.requestExperience(choice(action.allowed))
            is MagnetAction.SetFluids -> container.requestFluids(choice(action.allowed))
            is MagnetAction.SetComponents -> container.requestComponents(choice(action.allowed))
            is MagnetAction.SetRange -> container.requestRange(action.range)
        }
    }

    @Composable
    override fun Content(state: MagnetState, slots: ComposeMenuSlots<NetMagnetMenu>) {
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
                    BdSettingRow(text.working) {
                        BdToggle(state.working, { send(MagnetAction.SetWorking(it)) }, settingsOpen && state.workingEditable)
                    }
                    BdModeSetting(text.filter, text.filterOptions, state.filter.ordinal, settingsOpen && state.filterEditable) {
                        send(MagnetAction.SetFilter(FilterMode.entries[it]))
                    }
                    BdSettingRow(text.items) {
                        BdToggle(state.items, { send(MagnetAction.SetItems(it)) }, settingsOpen && state.itemsEditable)
                    }
                    BdSettingRow(text.experience) {
                        BdToggle(state.experience, { send(MagnetAction.SetExperience(it)) }, settingsOpen && state.experienceEditable)
                    }
                    BdSettingRow(text.fluids) {
                        BdToggle(state.fluids, { send(MagnetAction.SetFluids(it)) }, settingsOpen && state.fluidsEditable)
                    }
                    BdSettingRow(text.components) {
                        BdToggle(state.components, { send(MagnetAction.SetComponents(it)) }, settingsOpen && state.componentsEditable)
                    }
                    BdModeSetting(
                        text.range,
                        text.rangeOptions,
                        state.range,
                        settingsOpen && state.rangeEditable,
                        description = text.rangeHints[state.range],
                        cycle = true,
                    ) {
                        send(MagnetAction.SetRange(it))
                    }
                }
            }
        }
    }
}

data class MagnetState(
    val working: Boolean,
    val workingEditable: Boolean,
    val filter: FilterMode,
    val filterEditable: Boolean,
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

sealed interface MagnetAction {
    data class SetWorking(val on: Boolean) : MagnetAction

    data class SetFilter(val mode: FilterMode) : MagnetAction

    data class SetItems(val allowed: Boolean) : MagnetAction

    data class SetExperience(val allowed: Boolean) : MagnetAction

    data class SetFluids(val allowed: Boolean) : MagnetAction

    data class SetComponents(val allowed: Boolean) : MagnetAction

    data class SetRange(val range: Int) : MagnetAction
}

private val RANGES = listOf("lowest", "low", "mid", "high", "highest", "chunk")

private class MagnetText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
    val working = tr("ui.beyonddimensions.machine.working")
    val filter = tr("ui.beyonddimensions.machine.filter")
    val filterOptions = FilterMode.entries.map { tr("ui.beyonddimensions.machine.filter.${it.name.lowercase()}") }
    val items = tr("ui.beyonddimensions.machine.collect.items")
    val experience = tr("ui.beyonddimensions.machine.collect.experience")
    val fluids = tr("ui.beyonddimensions.machine.collect.fluids")
    val components = tr("ui.beyonddimensions.machine.collect.components")
    val range = tr("ui.beyonddimensions.machine.range")
    val rangeOptions = RANGES.map { tr("ui.beyonddimensions.machine.range.$it") }
    val rangeHints = RANGES.map { tr("ui.beyonddimensions.machine.range.magnet.$it") }
}
