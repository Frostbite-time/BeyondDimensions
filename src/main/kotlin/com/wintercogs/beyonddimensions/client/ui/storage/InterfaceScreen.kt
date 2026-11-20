package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.gameState
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络接口：每行标记槽下方是对应的缓存槽，标记决定从网络中保持哪些资源 */
class InterfaceScreen(menu: NetInterfaceBaseMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetInterfaceBaseMenu>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_INTERFACE.get()))
    private val flagRows = menu.flagSlotIds().chunked(9)
    private val storedRows = menu.resourceSlotIds().chunked(9)
    private val playerSlots = menu.playerSlotIds()
    private val text = InterfaceText(title.string)

    // 动态结构里的接口没有固定的相邻方块，设置页上没有面配置
    private val sides = machineSidesView(menu.access as? NetInterfaceBlockEntity)

    @Composable
    override fun Content(slots: ComposeMenuSlots<NetInterfaceBaseMenu>) {
        val colors = Bd.colors
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
                    BdSectionLabel(text.slots)
                    Spacer(Modifier.height(4.dp))
                    // 三组“标记行 + 缓存行”
                    for (row in flagRows.indices) {
                        Box(Modifier.bdSlotFrame(colors)) {
                            Column {
                                BdSlotGrid(slots, flagRows[row], 9)
                                BdSlotGrid(slots, storedRows[row], 9)
                            }
                        }
                        if (row < flagRows.lastIndex) Spacer(Modifier.height(3.dp))
                    }
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

private class InterfaceText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val slots = tr("ui.beyonddimensions.interface.slots")
}
