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
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.NetHopperMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
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

    override fun snapshot() = HopperState(machineSettingRows(container.machineSettings().snapshot()))

    override fun handle(action: HopperAction) {
        when (action) {
            is HopperAction.Setting -> container.requestSetting(action.pick.setting, action.pick.index)
        }
    }

    @Composable
    override fun Content(state: HopperState, slots: ComposeMenuSlots<NetHopperMenu>) {
        val page = rememberPageSelection()
        val settingsOpen = page.settings
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                tabs = {
                    BdRailTab(text.title, BdIcons.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                },
                pinned = {
                    BdRailTab(text.settings, BdIcons.Settings, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
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
                    BdMachineSettings(state.settings, settingsOpen) { send(HopperAction.Setting(it)) }
                }
            }
        }
    }
}

data class HopperState(val settings: List<MachineSettingRow>)

sealed interface HopperAction {
    data class Setting(val pick: MachineSettingPick) : HopperAction
}

private class HopperText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
}
