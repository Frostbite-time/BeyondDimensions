package com.wintercogs.beyonddimensions.client.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdIcons
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.at
import com.wintercogs.beyonddimensions.client.ui.base.endAt
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.common.menu.NetFurnaceMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.layout.OrePanel
import dev.compixel.ui.ore.theme.OreTheme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** 每一列的冶炼箭头（上、下两段）与燃料火焰 */
data class FurnaceProgress(val arrows: List<Pair<ItemIcon, ItemIcon>>, val flames: List<ItemIcon>)

class FurnaceLabels(val title: String, val input: String, val fuel: String, val places: List<SlotPlace>)

/**
 * 网络熔炉，布局与旧版相同：左右两列是输入与燃料过滤槽，中间是输入、冶炼进度、输出与燃料进度，下方是背包，
 * 全部槽位按菜单里的旧版坐标排布；右侧依次是弹出、接收、红石控制与自动整理按钮。
 */
class FurnaceScreen
private constructor(
    menu: NetFurnaceMenu,
    title: Component,
    controller: DeviceController<NetFurnaceMenu>,
    labels: FurnaceLabels,
) : BdInventoryScreen<NetFurnaceMenu>(menu, title, controller, { slots -> FurnaceView(slots, controller.ui, labels) }) {
    constructor(
        menu: NetFurnaceMenu,
        @Suppress("UNUSED_PARAMETER") inventory: Inventory,
        title: Component,
    ) : this(
        menu,
        title,
        DeviceController(
            menu,
            listOf(
                popTab({ menu.be.popMode }, { menu.be.popMode = it }),
                receiveTab({ menu.be.receiveMode }, { menu.be.receiveMode = it }),
                controlTab({ menu.be.controlMode }, { menu.be.controlMode = it }),
                autoSortTab({ menu.be.sortMode }, { menu.be.sortMode = it }),
            ),
            extra = { furnaceProgress(menu) },
        ),
        FurnaceLabels(
            title.string,
            tr("menu.label.beyonddimensions.input_filter_slots"),
            tr("menu.label.beyonddimensions.fuel_filter_slots"),
            menu.slots.map { SlotPlace(it.index, it.x, it.y) },
        ),
    )
}

/** 与旧版相同：冶炼进度从上往下取整到 19 行，燃料从下往上取整到 14 行 */
private fun furnaceProgress(menu: NetFurnaceMenu): FurnaceProgress {
    val be = menu.be
    fun rows(value: Int, total: Int, height: Int) = if (total > 0) (height * value / total.toFloat()).toInt().coerceIn(0, height) else 0
    val columns = 0 until be.capacity
    return FurnaceProgress(
        columns.map { i ->
            val rows = rows(be.cookTime[i], be.cookTimeTotal[i], 19)
            BdIcons.cookArrow(rows, lower = false) to BdIcons.cookArrow(rows, lower = true)
        },
        columns.map { i -> BdIcons.fuelFlame(rows(be.litTime[i], be.litDuration[i], 14)) },
    )
}

private const val FURNACE_WIDTH = 230
private const val FURNACE_HEIGHT = 210

@Composable
private fun FurnaceView(
    slots: ComposeMenuSlots<NetFurnaceMenu>,
    binding: UiBinding<DeviceState, Any>,
    labels: FurnaceLabels,
) {
    val state = binding.value
    val progress = state.extra as FurnaceProgress
    Box(Modifier.fillMaxSize().background(OreTheme.colors.backdrop), contentAlignment = Alignment.Center) {
        Box {
            OrePanel(
                "",
                Modifier.size(FURNACE_WIDTH.dp, FURNACE_HEIGHT.dp).then(slots.areaModifier()),
                showTitleBar = false,
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(Modifier.fillMaxSize()) {
                    OreText(labels.title, Modifier.at(8, 8), maxLines = 1)
                    OreText(labels.input, Modifier.at(6, 27), maxLines = 1)
                    OreText(labels.fuel, Modifier.endAt(224, 27), maxLines = 1)
                    SlotLayout(slots, labels.places)
                    // 图标内的箭头与火焰比图标原点多 1 像素：旧版箭头画在 (32 + 19i, 61)，火焰画在 (31 + 19i, 109)
                    for ((column, arrow) in progress.arrows.withIndex()) {
                        MinecraftItemIcon(arrow.first, Modifier.at(31 + column * 19, 61).size(16.dp))
                        MinecraftItemIcon(arrow.second, Modifier.at(31 + column * 19, 77).size(16.dp))
                    }
                    for ((column, flame) in progress.flames.withIndex())
                        MinecraftItemIcon(flame, Modifier.at(30 + column * 19, 108).size(16.dp))
                }
            }
            Tabs(state.tabs.withIndex().toList(), binding, Modifier.offset(x = (FURNACE_WIDTH - 2).dp, y = 6.dp))
        }
    }
}
