package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdThemes
import com.wintercogs.beyonddimensions.client.ui.base.at
import com.wintercogs.beyonddimensions.client.ui.base.slotAt
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.button.OreIconButton
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.theme.OreTheme
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

/** 合成区的高度，与旧版的合成贴图相同 */
private const val CRAFT_HEIGHT = 62

/**
 * 带合成区的存储终端：网络的合成模式与合成终端方块共用。合成终端方块没有切换合成模式的按钮。
 */
class CraftScreen<M : DimensionsCraftMenu>(menu: M, inventory: Inventory, title: Component) :
    StorageScreen<M>(
        menu,
        title,
        StorageController(menu, CRAFT_HEIGHT, craftToggle = menu !is DimensionsCraftMenuTerminal),
        StorageScreen.storageLabels(menu, inventory, title),
        CRAFT_HEIGHT,
        craftSection((menu.craftSlotStartIndex until menu.craftSlotEndIndex).toList(), menu.resultSlotIndex),
    )

private fun <M : DimensionsCraftMenu> craftSection(
    grid: List<Int>,
    result: Int,
): @Composable (ComposeMenuSlots<M>, StorageState, (StorageAction) -> Unit) -> Unit =
    { slots, state, send -> CraftSection(slots, grid, result, state, send) }

/**
 * 与旧版相同的排布，坐标相对合成区顶端：3x3 合成格从 (26, 3) 开始，上方三个 8×8 的小按钮在 x=81、90、99，
 * 箭头在格子与产物之间，产物槽是 (115, 16) 起的 26×26 大槽位。
 */
@Composable
private fun CraftSection(
    slots: ComposeMenuSlots<*>,
    grid: List<Int>,
    result: Int,
    state: StorageState,
    send: (StorageAction) -> Unit,
) {
    val top = storageUpperHeight(state.lines)
    OreTheme(id = BdThemes.Grid) {
        Box(Modifier.slotAt(26, top + 3)) { SlotGrid(slots, grid, columns = 3) }
        slots.Slot(result, Modifier.at(115, top + 16).size(26.dp))
    }
    OreTheme(id = BdThemes.Sidebar) {
        for ((index, entry) in state.craftButtons.withIndex()) {
            val (action, button) = entry
            OreIconButton(button.tooltip, { send(action) }, Modifier.at(81 + index * 9, top + 2).size(8.dp)) {
                MinecraftItemIcon(button.icon, Modifier.requiredSize(8.dp))
            }
        }
    }
    // 旧版的箭头以槽位的灰色画在 (87, 21) 起的 22×15 内
    OreIcon(OreGlyph.ArrowRight, Modifier.at(90, top + 20).size(16.dp), OreTheme.colors.slot)
}
