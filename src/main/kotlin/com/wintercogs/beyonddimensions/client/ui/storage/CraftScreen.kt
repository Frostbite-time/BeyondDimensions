package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdThemes
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal
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

/** 与旧版相同的排布：3x3 合成格，右侧三个小按钮与箭头，最右是产物 */
@Composable
private fun CraftSection(
    slots: ComposeMenuSlots<*>,
    grid: List<Int>,
    result: Int,
    state: StorageState,
    send: (StorageAction) -> Unit,
) {
    Row(Modifier.height(CRAFT_HEIGHT.dp).padding(start = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        OreTheme(id = BdThemes.Grid) {
            Column { for (row in grid.chunked(3)) Row { for (id in row) slots.Slot(id) } }
        }
        Spacer(Modifier.width(6.dp))
        OreTheme(id = BdThemes.Sidebar) {
            Column {
                for ((action, button) in state.craftButtons) OreIconButton(button.glyph, button.tooltip, { send(action) })
            }
        }
        Spacer(Modifier.width(8.dp))
        OreIcon(OreGlyph.ArrowRight, Modifier.size(16.dp), OreTheme.colors.edge)
        Spacer(Modifier.width(10.dp))
        OreTheme(id = BdThemes.Grid) { slots.Slot(result) }
    }
}
