package com.wintercogs.beyonddimensions.client.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
import com.wintercogs.beyonddimensions.client.ui.base.LocalBdScreen
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.math.abs


class NetEnergyScreen private constructor(
    menu: NetEnergyMenu,
    title: Component,
    private val ui: UiBinding<NetEnergyState, NetEnergyAction>,
    icon: ItemIcon,
    text: NetEnergyText
) : BdInventoryScreen<NetEnergyMenu>(
    menu,
    title,
    null,
    content = { slots -> NetEnergyContent(slots, ui.value, icon, text) { action -> ui.send(action) } }
) {
    constructor(
        menu: NetEnergyMenu,
        inventory: Inventory,
        title: Component
    ) : this(
        menu,
        title,
        UiBinding(
            NetEnergyState(
                PopMode.STOP,
                RedStoneControlMode.IGNORE,
                0, 0, 0
            )
        ),
        ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get())),
        NetEnergyText(title.string)
    )

    override fun inventoryTick() {
        super.inventoryTick()
        ui.drainActions { action ->
            when (action) {
                is NetEnergyAction.SetRedstoneMode -> container.requestRedstone(action.redstoneMode.ordinal)
                is NetEnergyAction.SetPopMode -> container.requestOutput(action.popMode.ordinal)
            }
        }

        ui.update(
            NetEnergyState(
                popMode = container.popMode(),
                redstoneMode = container.redStoneMode(),
                energyStored = container.energyStored(),
                energyCapacity = container.energyCapacity(),
                energyRate = container.energyRate(),
            )
        )
    }

    override fun removed() {
        val menuStillOpen = Minecraft.getInstance().player?.containerMenu === container
        try {
            super.removed()
        } finally {
            if (!menuStillOpen) {
                ui.close()
            }
        }
    }
}

@Composable
private fun NetEnergyContent(
    slots: ComposeMenuSlots<NetEnergyMenu>,
    state: NetEnergyState,
    icon: ItemIcon,
    text: NetEnergyText,
    onAction: (NetEnergyAction) -> Unit,
) {
    val colors = Bd.colors
    val screen = LocalBdScreen.current

    Box(
        Modifier.fillMaxSize().background(Color(0x400A1423)),
        contentAlignment = Alignment.Center,
    ) {
        BdWindow(
            Modifier.width(331.dp).then(slots.areaModifier())
        ) {
            BdHeader(
                icon = icon,
                overline = "BEYOND DIMENSIONS",
                title = text.title,
                status = null,
                onClose = screen::close,
            )

            Row(
                Modifier.padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 7.dp,
                    bottom = 8.dp,
                )
            ) {
                // 左侧：能量读数和玩家背包。
                Column(Modifier.width(163.dp)) {
                    BdSectionLabel(text.network) {
                        OreText(
                            "FE",
                            color = colors.faint,
                            style = Bd.caption,
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        OreText(
                            formatReadout(state.energyStored),
                            color = colors.text,
                            style = Bd.title,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(3.dp))
                        OreText(
                            "/ " + formatReadout(state.energyCapacity),
                            color = colors.muted,
                            maxLines = 1,
                        )
                        Spacer(Modifier.weight(1f))

                        val rateColor =
                            if (state.energyRate >= 0) colors.online
                            else colors.danger

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OreIcon(
                                if (state.energyRate >= 0) OreGlyph.ArrowUp
                                else OreGlyph.ArrowDown,
                                Modifier.size(6.dp),
                                color = rateColor,
                            )
                            Spacer(Modifier.width(2.dp))
                            OreText(
                                formatReadout(abs(state.energyRate)) + " FE/t",
                                color = rateColor,
                                style = Bd.caption,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    val fraction =
                        if (state.energyCapacity > 0) {
                            (
                                    state.energyStored.toDouble() /
                                            state.energyCapacity
                                    ).toFloat()
                        } else {
                            0f
                        }

                    BdMeter(
                        fraction,
                        Modifier.fillMaxWidth().height(11.dp),
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (tick in listOf("0", "25", "50", "75", "100%")) {
                            OreText(
                                tick,
                                color = colors.faint,
                                style = Bd.caption,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    BdSectionLabel(text.inventory)
                    Spacer(Modifier.height(4.dp))

                    Box(
                        Modifier.background(colors.line).padding(0.5.dp)
                    ) {
                        Column {
                            // 菜单槽位 0～26：背包三行。
                            repeat(3) { row ->
                                Row {
                                    repeat(9) { column ->
                                        BdSlot(slots, row * 9 + column)
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            // 菜单槽位 27～35：快捷栏。
                            Row {
                                repeat(9) { column ->
                                    BdSlot(slots, 27 + column)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // 右侧：输出开关和红石模式。
                Column(
                    Modifier.width(140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                )
                {
                    BdSectionLabel(text.settings)

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            OreText(
                                text.pop,
                                color = colors.text,
                                maxLines = 1,
                            )
                            OreText(
                                text.popHint,
                                color = colors.faint,
                                style = Bd.caption,
                                maxLines = 1,
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        BdToggle(
                            checked = state.popMode == PopMode.OPEN,
                            onCheckedChange = { checked ->
                                onAction(
                                    NetEnergyAction.SetPopMode(
                                        if (checked) PopMode.OPEN
                                        else PopMode.STOP
                                    )
                                )
                            },
                        )
                    }

                    Column(Modifier.fillMaxWidth()) {
                        OreText(
                            text.redstone,
                            color = colors.text,
                            maxLines = 1,
                        )

                        Spacer(Modifier.height(2.dp))

                        BdSegmented(
                            options = text.redstoneOptions,
                            selected = state.redstoneMode.ordinal,
                            onSelect = { index ->
                                onAction(
                                    NetEnergyAction.SetRedstoneMode(
                                        RedStoneControlMode.entries[index]
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fill = true,
                        )
                    }
                }
            }
        }
    }
}

data class NetEnergyState(
    val popMode: PopMode,
    val redstoneMode: RedStoneControlMode,
    val energyStored: Long,
    val energyCapacity: Long,
    val energyRate: Long
)

sealed interface NetEnergyAction {
    data class SetPopMode(val popMode: PopMode) : NetEnergyAction
    data class SetRedstoneMode(val redstoneMode: RedStoneControlMode) : NetEnergyAction
}

private class NetEnergyText(val title: String) {
    val syncing = tr("ui.beyonddimensions.status.syncing")
    val network = tr("ui.beyonddimensions.energy.network")
    val inventory = tr("ui.beyonddimensions.inventory")
    val settings = tr("ui.beyonddimensions.machine.settings")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions = RedStoneControlMode.entries.map {
        tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}")
    }
}