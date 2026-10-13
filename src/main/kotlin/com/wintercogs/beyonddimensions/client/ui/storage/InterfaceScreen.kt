package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络接口：每行标记槽下方是对应的缓存槽，标记决定从网络中保持哪些资源 */
class InterfaceScreen(menu: NetInterfaceBaseMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetInterfaceBaseMenu, InterfaceState, InterfaceAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_INTERFACE.get()))
    private val flagRows = menu.flagSlotIds().chunked(9)
    private val storedRows = menu.resourceSlotIds().chunked(9)
    private val playerSlots = menu.playerSlotIds()
    private val text = InterfaceText(title.string)

    // 动态结构里的接口没有固定的相邻方块，设置页上没有面配置
    private val sides = machineSidesView(menu.access as? NetInterfaceBlockEntity, inventory.player)

    override fun snapshot() = InterfaceState(machineSettingRows(container.machineSettings().snapshot(), sides))

    override fun handle(action: InterfaceAction) {
        when (action) {
            is InterfaceAction.Setting -> container.requestSetting(action.pick.setting, action.pick.index)
        }
    }

    @Composable
    override fun Content(state: InterfaceState, slots: ComposeMenuSlots<NetInterfaceBaseMenu>) {
        val colors = Bd.colors
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
                    BdSectionLabel(text.slots)
                    Spacer(Modifier.height(4.dp))
                    // 三组“标记行 + 缓存行”
                    for (row in flagRows.indices) {
                        Box(Modifier.background(colors[BdColors.line]).padding(0.5.dp)) {
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
                    BdSectionLabel(text.settings)
                    BdMachineSettings(state.settings, settingsOpen) { send(InterfaceAction.Setting(it)) }
                }
            }
        }
    }
}

data class InterfaceState(val settings: List<MachineSettingRow>)

sealed interface InterfaceAction {
    data class Setting(val pick: MachineSettingPick) : InterfaceAction
}

private class InterfaceText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val slots = tr("ui.beyonddimensions.interface.slots")
}
