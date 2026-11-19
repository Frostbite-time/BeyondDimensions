package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDItems
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings
import com.wintercogs.beyonddimensions.common.menu.XpExchangeMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.gameState
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

private val TARGET_RANGE = 0..XpExchangeSettings.MAX_TARGET_LEVEL

/** 经验棒：主页面上是目标等级与维持等级的设置，下面是用法说明与玩家背包 */
class XpExchangeScreen(menu: XpExchangeMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<XpExchangeMenu>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDItems.XP_EXCHANGE_ITEM.get()))
    private val playerSlots = menu.playerSlotIds()
    private val text = XpExchangeText(title.string)

    @Composable
    override fun Content(slots: ComposeMenuSlots<XpExchangeMenu>) {
        val colors = Bd.colors
        val page = rememberPageSelection()
        BdScreenFrame {
            BdTabbedWindow(
                slots.areaModifier(),
                width = SLOT_WINDOW_WIDTH.dp,
                header = { BdHeader(icon, text.title, ::requestClose) },
                tabs = {
                    BdRailTab(text.title, BdIcons.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                },
            ) {
                BdMainPage(page.main) {
                    val target by gameState { container.target() }
                    val targetEditable by gameState { container.targetEditable() }
                    val keep by gameState { container.keep() }
                    val keepEditable by gameState { container.keepEditable() }
                    BdSectionLabel(text.level)
                    Spacer(Modifier.height(5.dp))
                    BdSettingRow(text.target, text.targetHint) {
                        BdNumberEditor(
                            target,
                            { level -> gameThread.post { container.requestTarget(level) } },
                            TARGET_RANGE,
                            targetEditable,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    BdSettingRow(text.keep, text.keepHint) {
                        BdToggle(
                            keep,
                            { enabled -> gameThread.post { container.requestKeep(enabled) } },
                            keepEditable,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    BdSectionLabel(text.usage)
                    Spacer(Modifier.height(4.dp))
                    Column(Modifier.fillMaxWidth().background(colors[BdColors.surface]).padding(5.dp)) {
                        for (line in text.usageLines) OreText(line, color = colors[BdColors.muted], style = Bd.caption)
                    }
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
            }
        }
    }
}

private class XpExchangeText(val title: String) {
    val inventory = tr("ui.beyonddimensions.inventory")
    val level = tr("ui.beyonddimensions.xp.level")
    val usage = tr("ui.beyonddimensions.xp.usage")
    val usageLines = tr("tooltip.beyonddimensions.item.xp_exchange").split('\n')
    val keep = tr("ui.beyonddimensions.xp.keep")
    val keepHint = tr("ui.beyonddimensions.xp.keep.hint")
    val target = tr("ui.beyonddimensions.xp.target")
    val targetHint = tr("ui.beyonddimensions.xp.target.hint", XpExchangeSettings.MAX_TARGET_LEVEL)
}
