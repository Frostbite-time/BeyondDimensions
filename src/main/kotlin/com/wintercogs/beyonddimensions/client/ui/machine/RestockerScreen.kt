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
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetRestockerMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/**
 * 网络补货器：补货模板与玩家背包；设置页是总开关、模糊匹配与回收多余物品。
 * 模板与玩家背包一一对应：模板 0–8 是快捷栏，9–35 是主背包，36–40 是盔甲与副手，所以按背包的排布显示。
 */
class RestockerScreen(menu: NetRestockerMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetRestockerMenu, RestockerState, RestockerAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDItems.NET_RESTOCKER_ITEM.get()))
    private val templates = menu.flagSlotIds()
    private val playerSlots = menu.playerSlotIds()
    private val text = RestockerText(title.string)

    override fun snapshot() =
        RestockerState(
            working = container.working() == RedStoneControlMode.IGNORE,
            workingEditable = container.workingEditable(),
            fuzzy = container.matching() == FuzzyMode.ENABLE,
            fuzzyEditable = container.matchingEditable(),
            recycle = container.recycle() == ReceiveMode.OPEN,
            recycleEditable = container.recycleEditable(),
        )

    override fun handle(action: RestockerAction) {
        when (action) {
            // 总开关的第一个选项是“工作”，第二个是“停止”
            is RestockerAction.SetWorking -> container.requestWorking(if (action.on) 0 else 1)
            is RestockerAction.SetFuzzy -> container.requestMatching((if (action.on) FuzzyMode.ENABLE else FuzzyMode.DISABLE).ordinal)
            is RestockerAction.SetRecycle -> container.requestRecycle((if (action.on) ReceiveMode.OPEN else ReceiveMode.STOP).ordinal)
        }
    }

    @Composable
    override fun Content(state: RestockerState, slots: ComposeMenuSlots<NetRestockerMenu>) {
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
                    // 主背包的模板在上，快捷栏的在下，与玩家背包的排布相同
                    BdInventorySection(text.templates, templates.subList(9, 36) + templates.subList(0, 9), slots)
                    Spacer(Modifier.height(7.dp))
                    BdSlotSection(text.equipment, templates.drop(36), slots)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.working) {
                        BdToggle(
                            state.working,
                            { send(RestockerAction.SetWorking(it)) },
                            settingsOpen && state.workingEditable
                        )
                    }
                    BdSettingRow(text.fuzzy, text.fuzzyHint) {
                        BdToggle(
                            state.fuzzy,
                            { send(RestockerAction.SetFuzzy(it)) },
                            settingsOpen && state.fuzzyEditable
                        )
                    }
                    BdSettingRow(text.recycle, text.recycleHint) {
                        BdToggle(
                            state.recycle,
                            { send(RestockerAction.SetRecycle(it)) },
                            settingsOpen && state.recycleEditable
                        )
                    }
                }
            }
        }
    }
}

data class RestockerState(
    val working: Boolean,
    val workingEditable: Boolean,
    val fuzzy: Boolean,
    val fuzzyEditable: Boolean,
    val recycle: Boolean,
    val recycleEditable: Boolean,
)

sealed interface RestockerAction {
    data class SetWorking(val on: Boolean) : RestockerAction

    data class SetFuzzy(val on: Boolean) : RestockerAction

    data class SetRecycle(val on: Boolean) : RestockerAction
}

private class RestockerText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val templates = tr("ui.beyonddimensions.restocker.templates")
    val equipment = tr("ui.beyonddimensions.restocker.equipment")
    val working = tr("ui.beyonddimensions.machine.working")
    val fuzzy = tr("ui.beyonddimensions.machine.fuzzy")
    val fuzzyHint = tr("ui.beyonddimensions.machine.fuzzy.hint")
    val recycle = tr("ui.beyonddimensions.restocker.recycle")
    val recycleHint = tr("ui.beyonddimensions.restocker.recycle.hint")
}
