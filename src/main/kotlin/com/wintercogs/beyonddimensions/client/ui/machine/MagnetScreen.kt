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
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem
import com.wintercogs.beyonddimensions.common.machine.setting.PickupTarget
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 网络磁铁：过滤槽、例外槽与玩家背包；设置页的分组由机器类型声明 */
class MagnetScreen(menu: NetMagnetMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetMagnetMenu, MagnetState, MagnetAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDItems.NET_MAGNET_ITEM.get()))
    private val filterSlots = menu.flagSlotIds().take(NetMagnetItem.FILTER_SLOTS)
    private val exceptionSlots = menu.flagSlotIds().drop(NetMagnetItem.FILTER_SLOTS)
    private val playerSlots = menu.playerSlotIds()
    private val text = MagnetText(title.string)

    override fun snapshot(): MagnetState {
        val snapshot = container.machineSettings().snapshot()
        // 先送网络时例外的物品进背包，先送背包时进网络
        val target =
            snapshot.machineType()?.entry(BDMachineSettings.PICKUP_TARGET.get())?.valueIn(snapshot.settings())
                ?: PickupTarget.NETWORK_ONLY
        return MagnetState(
            settings = machineSettingRows(snapshot),
            exceptions = if (target.prefersNetwork()) text.toInventory else text.toNetwork,
            exceptionsHint = text.exceptionHints.getValue(target),
        )
    }

    override fun handle(action: MagnetAction) {
        when (action) {
            is MagnetAction.Setting -> container.requestSetting(action.pick.setting, action.pick.value)
        }
    }

    @Composable
    override fun Content(state: MagnetState, slots: ComposeMenuSlots<NetMagnetMenu>) {
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
                    BdSlotSection(state.exceptions, exceptionSlots, slots, hint = state.exceptionsHint)
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    BdMachineSettings(state.settings, settingsOpen) { send(MagnetAction.Setting(it)) }
                }
            }
        }
    }
}

data class MagnetState(
    val settings: List<MachineSettingRow>,
    /** 例外槽的标题与说明，随物品去向变化 */
    val exceptions: String,
    val exceptionsHint: String,
)

sealed interface MagnetAction {
    data class Setting(val pick: MachineSettingPick) : MagnetAction
}

private class MagnetText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
    val toInventory = tr("ui.beyonddimensions.magnet.exceptions.inventory")
    val toNetwork = tr("ui.beyonddimensions.magnet.exceptions.network")

    /** 例外物品实际怎么送，按物品去向各一句 */
    val exceptionHints =
        PickupTarget.entries.associateWith { tr("ui.beyonddimensions.magnet.exceptions.hint.${it.name.lowercase()}") }
}
