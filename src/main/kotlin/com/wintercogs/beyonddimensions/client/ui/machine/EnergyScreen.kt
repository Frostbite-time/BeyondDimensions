package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.math.abs

/** 维度网络能量通道：网络的能量读数与玩家背包；设置页是弹出模式与红石控制 */
class EnergyScreen(menu: NetEnergyMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetEnergyMenu, EnergyState, EnergyAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get()))
    private val playerSlots = menu.playerSlotIds()
    private val text = EnergyText(title.string)

    override fun snapshot() =
        EnergyState(
            pop = container.popMode() == PopMode.OPEN,
            redstone = container.redStoneMode(),
            stored = container.energyStored(),
            capacity = container.energyCapacity(),
            rate = container.energyRate(),
        )

    override fun handle(action: EnergyAction) {
        when (action) {
            is EnergyAction.SetPop -> container.requestOutput((if (action.open) PopMode.OPEN else PopMode.STOP).ordinal)
            is EnergyAction.SetRedstone -> container.requestRedstone(action.mode.ordinal)
        }
    }

    @Composable
    override fun Content(state: EnergyState, slots: ComposeMenuSlots<NetEnergyMenu>) {
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
                    EnergyReadout(state, text)
                    Spacer(Modifier.height(8.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.pop, text.popHint) {
                        BdToggle(state.pop, { send(EnergyAction.SetPop(it)) }, enabled = settingsOpen)
                    }
                    BdModeSetting(text.redstone, text.redstoneOptions, state.redstone.ordinal, enabled = settingsOpen) {
                        send(EnergyAction.SetRedstone(RedStoneControlMode.entries[it]))
                    }
                }
            }
        }
    }
}

data class EnergyState(
    val pop: Boolean,
    val redstone: RedStoneControlMode,
    val stored: Long,
    val capacity: Long,
    val rate: Long,
) {
    val fraction: Float
        get() = if (capacity > 0) (stored.toDouble() / capacity).toFloat() else 0f
}

sealed interface EnergyAction {
    data class SetPop(val open: Boolean) : EnergyAction

    data class SetRedstone(val mode: RedStoneControlMode) : EnergyAction
}

private class EnergyText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val network = tr("ui.beyonddimensions.energy.network")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}

private val TICKS = listOf("0", "25", "50", "75", "100%")

/** 网络能量：存量与容量、每刻变化，以及分段的能量条 */
@Composable
private fun EnergyReadout(state: EnergyState, text: EnergyText) {
    val colors = Bd.colors
    BdSectionLabel(text.network) { OreText("FE", color = colors.faint, style = Bd.caption) }
    Spacer(Modifier.height(5.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        OreText(formatReadout(state.stored), color = colors.text, style = Bd.title, maxLines = 1)
        Spacer(Modifier.width(3.dp))
        OreText("/ " + formatReadout(state.capacity), color = colors.muted, style = Bd.body, maxLines = 1)
        Spacer(Modifier.weight(1f))
        val rateColor = if (state.rate >= 0) colors.online else colors.danger
        Row(verticalAlignment = Alignment.CenterVertically) {
            OreIcon(
                if (state.rate >= 0) OreGlyph.ArrowUp else OreGlyph.ArrowDown,
                Modifier.size(6.dp),
                color = rateColor
            )
            Spacer(Modifier.width(2.dp))
            OreText(formatReadout(abs(state.rate)) + " FE/t", color = rateColor, style = Bd.caption, maxLines = 1)
        }
    }
    Spacer(Modifier.height(4.dp))
    BdMeter(state.fraction, Modifier.fillMaxWidth().height(11.dp))
    Spacer(Modifier.height(2.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (tick in TICKS) OreText(tick, color = colors.faint, style = Bd.caption, maxLines = 1)
    }
}
