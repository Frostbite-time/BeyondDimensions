package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.machine.MachineController
import com.wintercogs.beyonddimensions.client.ui.machine.MachineLayout
import com.wintercogs.beyonddimensions.client.ui.machine.MachineScreen
import com.wintercogs.beyonddimensions.client.ui.machine.ModeSetting
import com.wintercogs.beyonddimensions.client.ui.machine.ToggleSetting
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu
import dev.compixel.forge.slots.ComposeMenuSlots
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/**
 * 网络接口：每行标记槽下方是对应的缓存槽，标记决定从网络中保持哪些资源。
 */
class InterfaceScreen private constructor(menu: NetInterfaceBaseMenu, title: Component, layout: MachineLayout) :
    MachineScreen<NetInterfaceBaseMenu>(
        menu,
        title,
        MachineController(menu::ready, settings(menu)),
        layout,
        { _, slots -> InterfaceSlots(layout, slots) },
    ) {
    constructor(
        menu: NetInterfaceBaseMenu,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, MachineLayout(menu, BDBlocks.NET_INTERFACE.get(), title.string))

    private companion object {
        fun settings(menu: NetInterfaceBaseMenu) =
            listOf(
                ToggleSetting(
                    tr("ui.beyonddimensions.machine.pop"),
                    if (menu.synchronizedCanConfigurePop) tr("ui.beyonddimensions.interface.pop.hint")
                    else tr("tooltip.button.beyonddimensions.popmode_mounted_unavailable"),
                    { menu.synchronizedPopMode == PopMode.OPEN },
                    { menu.synchronizedCanConfigurePop },
                    {
                        val mode = if (it) PopMode.OPEN else PopMode.STOP
                        menu.commands().mode(NetInterfaceBaseMenu.SETTING_POP, mode.ordinal)
                    },
                ),
                ModeSetting(
                    tr("ui.beyonddimensions.machine.redstone"),
                    RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") },
                    { menu.synchronizedControlMode.ordinal },
                    { true },
                    { menu.commands().mode(NetInterfaceBaseMenu.SETTING_REDSTONE, it) },
                ),
                ToggleSetting(
                    tr("ui.beyonddimensions.machine.fuzzy"),
                    tr("ui.beyonddimensions.machine.fuzzy.hint"),
                    { menu.synchronizedFuzzyMode == FuzzyMode.ENABLE },
                    { true },
                    {
                        val mode = if (it) FuzzyMode.ENABLE else FuzzyMode.DISABLE
                        menu.commands().mode(NetInterfaceBaseMenu.SETTING_FUZZY, mode.ordinal)
                    },
                ),
            )
    }
}

/** 三组“标记行 + 缓存行” */
@Composable
private fun InterfaceSlots(layout: MachineLayout, slots: ComposeMenuSlots<NetInterfaceBaseMenu>) {
    val colors = Bd.colors
    BdSectionLabel(tr("ui.beyonddimensions.interface.slots"))
    Spacer(Modifier.height(4.dp))
    val flags = layout.flagSlots.chunked(9)
    val stored = layout.resourceSlots.chunked(9)
    Column {
        for (row in flags.indices) {
            Box(Modifier.background(colors.line).padding(0.5.dp)) {
                Column {
                    BdSlotGrid(slots, flags[row], 9)
                    stored.getOrNull(row)?.let { BdSlotGrid(slots, it, 9) }
                }
            }
            if (row < flags.lastIndex) Spacer(Modifier.height(3.dp))
        }
    }
    Spacer(Modifier.height(7.dp))
}
