package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.inventory.OreSlot

const val SLOT_PITCH = 18

/** 放九列槽位的标准窗口宽度：页签竖条、页面两侧的留白、九格与外圈细线，再加窗口边框 */
const val SLOT_WINDOW_WIDTH = SIDE_RAIL_WIDTH + PAGE_PADDING_X * 2 + 9 * SLOT_PITCH + 1 + 2

/**
 * 一个原生槽位：主题化的槽底，悬停时加上角标；虚拟资源的数量以 BD 的数量标签显示。
 *
 * 悬停只有角标这一种效果，两种槽位都按 CompixelUI 给出的 hovered 画，也就是点击会落到的那一格。
 * Ore 外观的槽位悬停时还会铺一层悬停色，主题里把它设成与槽底相同，所以看不出来。
 *
 * 标记槽与其他槽位用同样的凹槽，只在槽底铺一层强调色。Ore 的标记样式用通用描边色画凹槽的暗边，
 * 玻璃主题的描边色是半透明白，相邻两格的亮边会连成双线，交点处还留下缺角。
 */
@Composable
fun BdSlot(slots: ComposeMenuSlots<*>, id: Int, modifier: Modifier = Modifier) {
    if ((slots.adapter as? BdSlotAdapter<*>)?.isResource(id) == true) {
        slots.Slot(id, modifier.size(SLOT_PITCH.dp)) { slot ->
            OreSlot(Modifier.matchParentSize(), contentModifier = Modifier.size(16.dp)) {
                if (slot.marked) Box(Modifier.matchParentSize().background(Bd.colors.accentSoft))
                slot.icon?.let { MinecraftItemIcon(it, Modifier.fillMaxSize()) }
            }
            if (slot.amount.isNotEmpty())
                BdAmountPill(slot.amount, Modifier.align(Alignment.BottomEnd).padding(end = 1.dp, bottom = 1.dp))
            if (slot.hovered) Box(Modifier.matchParentSize().brackets(Bd.colors.accent, arm = 4.dp))
        }
        return
    }
    // 原版槽位沿用 Ore 的外观与数量文字，角标叠在上面
    slots.Slot(
        id,
        modifier.size(SLOT_PITCH.dp),
        overlay = { slot -> if (slot.hovered) Box(Modifier.matchParentSize().brackets(Bd.colors.accent, arm = 4.dp)) },
    )
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

/** 带标题的一组槽位，外面一圈细线 */
@Composable
fun BdSlotSection(title: String, ids: List<Int>, slots: ComposeMenuSlots<*>, columns: Int = 9) {
    BdSectionLabel(title)
    Spacer(Modifier.height(4.dp))
    Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids, columns) }
}

/** 带标题的玩家背包 */
@Composable
fun BdInventorySection(title: String, playerSlots: List<Int>, slots: ComposeMenuSlots<*>) {
    BdSectionLabel(title)
    Spacer(Modifier.height(4.dp))
    Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) { BdPlayerInventory(slots, playerSlots) }
}

// 槽位编号在游戏线程构造界面时取出

/** 玩家背包的槽位：前 27 个是主背包，后 9 个是快捷栏 */
fun BDBaseMenu.playerSlotIds(): List<Int> = (inventoryStartIndex until inventoryEndIndex).toList()

/** 标记槽：只表示资源种类的虚拟槽位 */
fun BDBaseMenu.flagSlotIds(): List<Int> = slots.filter { it is AbstractStackTypedSlot && it.isFake }.map { it.index }

/** 存放虚拟资源的槽位 */
fun BDBaseMenu.resourceSlotIds(): List<Int> =
    slots.filter { it is AbstractStackTypedSlot && !it.isFake }.map { it.index }
