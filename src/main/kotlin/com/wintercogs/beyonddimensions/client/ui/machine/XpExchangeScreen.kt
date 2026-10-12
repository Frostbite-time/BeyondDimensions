package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

private val TARGET_RANGE = 0..XpExchangeSettings.MAX_TARGET_LEVEL

/** 经验棒：主页面上是目标等级与维持等级的设置，下面是用法说明与玩家背包 */
class XpExchangeScreen(menu: XpExchangeMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<XpExchangeMenu, XpExchangeState, XpExchangeAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDItems.XP_EXCHANGE_ITEM.get()))
    private val playerSlots = menu.playerSlotIds()
    private val text = XpExchangeText(title.string)

    override fun snapshot() =
        XpExchangeState(
            keep = container.keep(),
            keepEditable = container.keepEditable(),
            target = container.target(),
            targetEditable = container.targetEditable(),
        )

    override fun handle(action: XpExchangeAction) {
        when (action) {
            is XpExchangeAction.SetKeep -> container.requestKeep(action.keep)
            is XpExchangeAction.SetTarget -> container.requestTarget(action.level)
        }
    }

    @Composable
    override fun Content(state: XpExchangeState, slots: ComposeMenuSlots<XpExchangeMenu>) {
        val colors = Bd.colors
        val page = rememberPageSelection()
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                rail = {
                    BdRailTab(text.title, BdGlyphs.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                },
            ) {
                BdMainPage(page.main) {
                    BdSectionLabel(text.level)
                    Spacer(Modifier.height(5.dp))
                    BdSettingRow(text.target, text.targetHint) {
                        BdNumberEditor(
                            state.target,
                            { send(XpExchangeAction.SetTarget(it)) },
                            TARGET_RANGE,
                            state.targetEditable,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    BdSettingRow(text.keep, text.keepHint) {
                        BdToggle(state.keep, { send(XpExchangeAction.SetKeep(it)) }, state.keepEditable)
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

data class XpExchangeState(
    val keep: Boolean,
    val keepEditable: Boolean,
    val target: Int,
    val targetEditable: Boolean,
)

sealed interface XpExchangeAction {
    data class SetKeep(val keep: Boolean) : XpExchangeAction

    data class SetTarget(val level: Int) : XpExchangeAction
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
