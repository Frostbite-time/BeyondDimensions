package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.kit.BdAmountPill
import com.wintercogs.beyonddimensions.client.ui.kit.brackets
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.inventory.OreSlot

const val SLOT_PITCH = 18

/** 一个原生槽位：主题化的槽底，悬停时加上角标；虚拟资源的数量以 BD 的数量标签显示 */
@Composable
fun BdSlot(slots: ComposeMenuSlots<*>, id: Int, modifier: Modifier = Modifier) {
    if ((slots.adapter as? BdSlotAdapter<*>)?.isResource(id) == true) {
        slots.Slot(id, modifier.size(SLOT_PITCH.dp)) { slot ->
            OreSlot(
                Modifier.matchParentSize(),
                marked = slot.marked,
                highlighted = slot.highlighted,
                contentModifier = Modifier.size(16.dp),
            ) {
                slot.icon?.let { MinecraftItemIcon(it, Modifier.fillMaxSize()) }
            }
            if (slot.amount.isNotEmpty())
                BdAmountPill(slot.amount, Modifier.align(Alignment.BottomEnd).padding(end = 1.dp, bottom = 1.dp))
            if (slot.hovered) Box(Modifier.matchParentSize().brackets(Bd.colors.accent, arm = 4.dp))
        }
        return
    }
    // 原版槽位沿用 Ore 的外观与数量文字
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier
            .size(SLOT_PITCH.dp)
            .hoverable(interaction)
            .then(if (hovered) Modifier.brackets(Bd.colors.accent, arm = 4.dp) else Modifier)
    ) {
        slots.Slot(id, Modifier.matchParentSize())
    }
}

/** 按列排布的一组槽位；不足一行的末尾留空 */
@Composable
fun BdSlotGrid(slots: ComposeMenuSlots<*>, ids: List<Int>, columns: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        for (row in ids.chunked(columns)) {
            Row { for (id in row) BdSlot(slots, id) }
        }
    }
}

/** 玩家背包：三行主背包，稍作间隔后是快捷栏 */
@Composable
fun BdPlayerInventory(slots: ComposeMenuSlots<*>, playerSlots: List<Int>, modifier: Modifier = Modifier) {
    val main = playerSlots.take(27)
    val hotbar = playerSlots.drop(27)
    Column(modifier) {
        BdSlotGrid(slots, main, 9)
        Spacer(Modifier.height(3.dp))
        BdSlotGrid(slots, hotbar, 9)
    }
}

/** 包住一组槽位的浅色托盘，使槽位与窗口底色区分 */
@Composable
fun BdSlotTray(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(Bd.colors.surface).padding(1.dp)) { content() }
}
