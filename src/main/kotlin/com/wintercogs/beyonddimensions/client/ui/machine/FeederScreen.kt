package com.wintercogs.beyonddimensions.client.ui.machine

import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
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
        val page = rememberPageSelection()
        val settingsOpen = page.settings
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                rail = {
                    BdRailTab(text.title, BdGlyphs.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                    Spacer(Modifier.weight(1f))
                    BdRailTab(text.settings, OreGlyph.Gear.art, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
                },
            ) {
                BdMainPage(page.main) {
                    BdSlotSection(text.foods, foodSlots, slots)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.working) {
                        BdToggle(
                            state.working,
                            { send(FeederAction.SetWorking(it)) },
                            settingsOpen && state.workingEditable
                        )
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
