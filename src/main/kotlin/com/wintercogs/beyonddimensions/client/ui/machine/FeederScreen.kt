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
import com.wintercogs.beyonddimensions.common.menu.NetFeederMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
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

    override fun snapshot() = FeederState(machineSettingRows(container.machineSettings().snapshot()))

    override fun handle(action: FeederAction) {
        when (action) {
            is FeederAction.Setting -> container.requestSetting(action.pick.setting, action.pick.index)
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
                tabs = {
                    BdRailTab(text.title, BdIcons.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                },
                pinned = {
                    BdRailTab(text.settings, BdIcons.Settings, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
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
                    BdMachineSettings(state.settings, settingsOpen) { send(FeederAction.Setting(it)) }
                }
            }
        }
    }
}

data class FeederState(val settings: List<MachineSettingRow>)

sealed interface FeederAction {
    data class Setting(val pick: MachineSettingPick) : FeederAction
}

private class FeederText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val foods = tr("ui.beyonddimensions.feeder.foods")
}
