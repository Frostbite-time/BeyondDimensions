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

    override fun snapshot() = MagnetState(machineSettingRows(container.machineSettings().snapshot()))

    override fun handle(action: MagnetAction) {
        when (action) {
            is MagnetAction.Setting -> container.requestSetting(action.pick.setting, action.pick.index)
        }
    }

    @Composable
    override fun Content(state: MagnetState, slots: ComposeMenuSlots<NetMagnetMenu>) {
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
                    BdSlotSection(text.filters, filterSlots, slots)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdMachineSettings(state.settings, settingsOpen) { send(MagnetAction.Setting(it)) }
                }
            }
        }
    }
}

data class MagnetState(val settings: List<MachineSettingRow>)

sealed interface MagnetAction {
    data class Setting(val pick: MachineSettingPick) : MagnetAction
}

private class MagnetText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
}
