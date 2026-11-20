package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.registry.BDRegistries
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings
import com.wintercogs.beyonddimensions.common.menu.NetStockEmitterMenu
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.gameState
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 存量发信器：标记要监测的资源，设定发信条件与数量，下面是玩家背包；设置页是模糊匹配 */
class StockEmitterScreen(menu: NetStockEmitterMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetStockEmitterMenu>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_STOCK_EMITTER.get()))
    private val markSlot = menu.flagSlotIds().single()
    private val playerSlots = menu.playerSlotIds()
    private val text = StockEmitterText(title.string)
    private val emitSetting = BDRegistries.MACHINE_SETTINGS.getKey(BDMachineSettings.EMIT.get())
    private val editable = !inventory.player.isSpectator

    private fun readMark(): String? {
        val key = (container.getSlot(markSlot) as AbstractStackTypedSlot).stack.key()
        return when {
            key.isEmpty -> null
            key is ItemStackKey -> key.readOnlyStack.hoverName.string
            else -> key.render.getDisplayName(key).string
        }
    }

    @Composable
    override fun Content(slots: ComposeMenuSlots<NetStockEmitterMenu>) {
        val colors = Bd.colors
        val page = rememberPageSelection()
        val settingsOpen = page.settings
        BdScreenFrame {
            BdTabbedWindow(
                slots.areaModifier(),
                width = SLOT_WINDOW_WIDTH.dp,
                header = { BdHeader(icon, text.title, ::requestClose) },
                tabs = {
                    BdRailTab(text.title, BdIcons.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                },
                pinned = {
                    BdRailTab(text.settings, BdIcons.Settings, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
                },
            ) {
                BdMainPage(page.main) {
                    val mark by gameState { readMark() }
                    val amount by gameState { container.amount() }
                    val threshold by gameState { container.threshold() }
                    val emitting by gameState { container.emitting() }
                    val condition by gameState {
                        // 发信条件放在主页面
                        machineSettingRows(
                            container.machineSettings().snapshot()
                        ).firstOrNull { it.setting == emitSetting }
                    }
                    BdSectionLabel(text.watched)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.bdSlotFrame(colors)) { BdSlot(slots, markSlot) }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            OreText(
                                mark ?: text.unmarked,
                                color = colors[if (mark != null) BdColors.text else BdColors.faint],
                                maxLines = 1,
                            )
                            OreText(
                                tr("ui.beyonddimensions.emitter.amount", formatExact(amount)),
                                color = colors[BdColors.muted],
                                style = Bd.caption,
                                maxLines = 1,
                            )
                            Signal(emitting, text)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    BdSectionLabel(text.condition)
                    Spacer(Modifier.height(4.dp))
                    condition?.let {
                        BdMachineSettings(listOf(it), page.main) { pick ->
                            gameThread.post { container.requestSetting(pick.setting, pick.value) }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    BdSettingRow(text.threshold) {
                        BdNumberEditor(
                            threshold,
                            { value -> gameThread.post { container.requestThreshold(value) } },
                            0L..Long.MAX_VALUE,
                            editable,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    val settings by gameState {
                        machineSettingRows(container.machineSettings().snapshot()).filter { it.setting != emitSetting }
                    }
                    BdSectionLabel(text.settings)
                    BdMachineSettings(settings, settingsOpen) { pick ->
                        gameThread.post { container.requestSetting(pick.setting, pick.value) }
                    }
                }
            }
        }
    }
}

/** 是否正在输出红石信号：亮起的小方块与一行说明 */
@Composable
private fun Signal(emitting: Boolean, text: StockEmitterText) {
    val colors = Bd.colors
    val tint = if (emitting) colors[BdColors.online] else colors[BdColors.faint]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(5.dp)
                .background(if (emitting) tint else colors[BdColors.sunken])
                .border(1.dp, if (emitting) tint else colors[BdColors.lineStrong])
        )
        Spacer(Modifier.width(3.dp))
        OreText(if (emitting) text.on else text.off, color = tint, style = Bd.caption, maxLines = 1)
    }
}

private class StockEmitterText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val watched = tr("ui.beyonddimensions.emitter.watched")
    val unmarked = tr("ui.beyonddimensions.emitter.unmarked")
    val on = tr("ui.beyonddimensions.emitter.on")
    val off = tr("ui.beyonddimensions.emitter.off")
    val condition = tr("ui.beyonddimensions.emitter.condition")
    val threshold = tr("ui.beyonddimensions.emitter.threshold")
}
