package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.common.init.BDItems
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

    override fun snapshot() = RestockerState(machineSettingRows(container.machineSettings().snapshot()))

    override fun handle(action: RestockerAction) {
        when (action) {
            is RestockerAction.Setting -> container.requestSetting(action.pick.setting, action.pick.index)
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
                    BdMachineSettings(state.settings, settingsOpen) { send(RestockerAction.Setting(it)) }
                }
            }
        }
    }
}

data class RestockerState(val settings: List<MachineSettingRow>)

sealed interface RestockerAction {
    data class Setting(val pick: MachineSettingPick) : RestockerAction
}

private class RestockerText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val templates = tr("ui.beyonddimensions.restocker.templates")
    val equipment = tr("ui.beyonddimensions.restocker.equipment")
}
