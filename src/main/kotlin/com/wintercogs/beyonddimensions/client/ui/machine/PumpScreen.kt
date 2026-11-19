package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.gameState
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络泵：过滤槽与玩家背包；设置页是从哪些面抽取、过滤模式与红石控制 */
class PumpScreen(menu: NetPumpMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetPumpMenu>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_PUMP_BLOCK.get()))
    private val filterSlots = menu.flagSlotIds()
    private val playerSlots = menu.playerSlotIds()
    private val text = PumpText(title.string)
    private val sides = machineSidesView(menu.be)

    @Composable
    override fun Content(slots: ComposeMenuSlots<NetPumpMenu>) {
        val page = rememberPageSelection()
        val settingsOpen = page.settings
        BdScreenFrame {
            BdTabbedWindow(
                slots.areaModifier(),
                width = SLOT_WINDOW_WIDTH.dp,
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
                    val settings by gameState { machineSettingRows(container.machineSettings().snapshot(), sides) }
                    BdSectionLabel(text.settings)
                    BdMachineSettings(settings, settingsOpen) { pick ->
                        gameThread.post { container.requestSetting(pick.setting, pick.value) }
                    }
                }
            }
        }
    }
}

private class PumpText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
}
