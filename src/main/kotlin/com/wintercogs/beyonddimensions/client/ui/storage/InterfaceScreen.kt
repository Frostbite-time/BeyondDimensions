package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/**
 * 网络接口：每行标记槽下方是对应的缓存槽，标记决定从网络中保持哪些资源；
 * 设置页是弹出模式（装在某些方块上时不可用）、红石控制与模糊匹配。
 */
class InterfaceScreen(menu: NetInterfaceBaseMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetInterfaceBaseMenu, InterfaceState, InterfaceAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_INTERFACE.get()))
    private val flagRows = menu.flagSlotIds().chunked(9)
    private val storedRows = menu.resourceSlotIds().chunked(9)
    private val playerSlots = menu.playerSlotIds()
    private val text = InterfaceText(title.string)

    override fun snapshot() =
        InterfaceState(
            pop = container.synchronizedPopMode == PopMode.OPEN,
            popConfigurable = container.synchronizedCanConfigurePop,
            redstone = container.synchronizedControlMode,
            fuzzy = container.synchronizedFuzzyMode == FuzzyMode.ENABLE,
        )

    override fun handle(action: InterfaceAction) {
        val commands = container.commands()
        when (action) {
            is InterfaceAction.SetPop ->
                commands.mode(NetInterfaceBaseMenu.SETTING_POP, (if (action.on) PopMode.OPEN else PopMode.STOP).ordinal)

            is InterfaceAction.SetRedstone -> commands.mode(NetInterfaceBaseMenu.SETTING_REDSTONE, action.mode.ordinal)
            is InterfaceAction.SetFuzzy ->
                commands.mode(
                    NetInterfaceBaseMenu.SETTING_FUZZY,
                    (if (action.on) FuzzyMode.ENABLE else FuzzyMode.DISABLE).ordinal
                )
        }
    }

    @Composable
    override fun Content(state: InterfaceState, slots: ComposeMenuSlots<NetInterfaceBaseMenu>) {
        val colors = Bd.colors
        var settingsOpen by remember { mutableStateOf(false) }
        slots.Interaction(enabled = !settingsOpen)
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                rail = {
                    BdRailTab(text.title, BdGlyphs.Main, selected = !settingsOpen) { settingsOpen = false }
                    Spacer(Modifier.weight(1f))
                    BdRailTab(text.settings, OreGlyph.Gear.art, selected = settingsOpen) { settingsOpen = true }
                },
            ) {
                BdMainPage(!settingsOpen) {
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
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.pop, if (state.popConfigurable) text.popHint else text.popUnavailable) {
                        BdToggle(state.pop, { send(InterfaceAction.SetPop(it)) }, settingsOpen && state.popConfigurable)
                    }
                    BdModeSetting(text.redstone, text.redstoneOptions, state.redstone.ordinal, settingsOpen) {
                        send(InterfaceAction.SetRedstone(RedStoneControlMode.entries[it]))
                    }
                    BdSettingRow(text.fuzzy, text.fuzzyHint) {
                        BdToggle(state.fuzzy, { send(InterfaceAction.SetFuzzy(it)) }, settingsOpen)
                    }
                }
            }
        }
    }
}

data class InterfaceState(
    val pop: Boolean,
    val popConfigurable: Boolean,
    val redstone: RedStoneControlMode,
    val fuzzy: Boolean,
)

sealed interface InterfaceAction {
    data class SetPop(val on: Boolean) : InterfaceAction

    data class SetRedstone(val mode: RedStoneControlMode) : InterfaceAction

    data class SetFuzzy(val on: Boolean) : InterfaceAction
}

private class InterfaceText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val slots = tr("ui.beyonddimensions.interface.slots")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.interface.pop.hint")
    val popUnavailable = tr("tooltip.button.beyonddimensions.popmode_mounted_unavailable")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
    val fuzzy = tr("ui.beyonddimensions.machine.fuzzy")
    val fuzzyHint = tr("ui.beyonddimensions.machine.fuzzy.hint")
}
