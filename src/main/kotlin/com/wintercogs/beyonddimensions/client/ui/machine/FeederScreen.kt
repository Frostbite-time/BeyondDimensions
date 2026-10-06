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
import com.wintercogs.beyonddimensions.common.machine.FeederMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetFeederMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络喂食器：可喂的食物与玩家背包；设置页是总开关与喂食方式 */
class FeederScreen(menu: NetFeederMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetFeederMenu, FeederState, FeederAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDItems.NET_FEEDER_ITEM.get()))
    private val foodSlots = menu.flagSlotIds()
    private val playerSlots = menu.playerSlotIds()
    private val text = FeederText(title.string)

    override fun snapshot() =
        FeederState(
            working = container.working() == RedStoneControlMode.IGNORE,
            workingEditable = container.workingEditable(),
            feeding = container.feeding(),
            feedingEditable = container.feedingEditable(),
        )

    override fun handle(action: FeederAction) {
        when (action) {
            // 总开关的第一个选项是“工作”，第二个是“停止”
            is FeederAction.SetWorking -> container.requestWorking(if (action.on) 0 else 1)
            is FeederAction.SetFeeding -> container.requestFeeding(action.mode.ordinal)
        }
    }

    @Composable
    override fun Content(state: FeederState, slots: ComposeMenuSlots<NetFeederMenu>) {
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
                    BdSlotSection(text.foods, foodSlots, slots)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.working) {
                        BdToggle(state.working, { send(FeederAction.SetWorking(it)) }, settingsOpen && state.workingEditable)
                    }
                    BdModeSetting(
                        text.feeding,
                        text.feedingOptions,
                        state.feeding.ordinal,
                        settingsOpen && state.feedingEditable,
                        description = text.feedingHints[state.feeding.ordinal],
                        cycle = true,
                    ) {
                        send(FeederAction.SetFeeding(FeederMode.entries[it]))
                    }
                }
            }
        }
    }
}

data class FeederState(
    val working: Boolean,
    val workingEditable: Boolean,
    val feeding: FeederMode,
    val feedingEditable: Boolean,
)

sealed interface FeederAction {
    data class SetWorking(val on: Boolean) : FeederAction

    data class SetFeeding(val mode: FeederMode) : FeederAction
}

private class FeederText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val foods = tr("ui.beyonddimensions.feeder.foods")
    val working = tr("ui.beyonddimensions.machine.working")
    val feeding = tr("ui.beyonddimensions.feeder.mode")
    val feedingOptions = FeederMode.entries.map { tr("ui.beyonddimensions.feeder.mode.${it.name.lowercase()}") }
    val feedingHints = FeederMode.entries.map { tr("ui.beyonddimensions.feeder.mode.${it.name.lowercase()}.hint") }
}
