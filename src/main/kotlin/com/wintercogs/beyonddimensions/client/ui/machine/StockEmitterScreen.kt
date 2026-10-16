package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/** 存量发信器：标记要监测的资源，设定发信条件与数量，下面是玩家背包；设置页是模糊匹配 */
class StockEmitterScreen(menu: NetStockEmitterMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetStockEmitterMenu, StockEmitterState, StockEmitterAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_STOCK_EMITTER.get()))
    private val markSlot = menu.flagSlotIds().single()
    private val playerSlots = menu.playerSlotIds()
    private val text = StockEmitterText(title.string)
    private val emitSetting = BDRegistries.MACHINE_SETTINGS.getKey(BDMachineSettings.EMIT.get())
    private val editable = !inventory.player.isSpectator

    override fun snapshot(): StockEmitterState {
        // 发信条件放在主页面，设置页只留其余的设置
        val (condition, settings) = machineSettingRows(
            container.machineSettings().snapshot()
        ).partition { it.setting == emitSetting }
        val key = (container.getSlot(markSlot) as AbstractStackTypedSlot).stack.key()
        val mark =
            when {
                key.isEmpty -> null
                key is ItemStackKey -> key.readOnlyStack.hoverName.string
                else -> key.render.getDisplayName(key).string
            }
        return StockEmitterState(
            mark = mark,
            amount = tr("ui.beyonddimensions.emitter.amount", formatExact(container.amount())),
            threshold = container.threshold(),
            emitting = container.emitting(),
            condition = condition.firstOrNull(),
            settings = settings,
        )
    }

    override fun handle(action: StockEmitterAction) {
        when (action) {
            is StockEmitterAction.Setting -> container.requestSetting(action.pick.setting, action.pick.value)
            is StockEmitterAction.Threshold -> container.requestThreshold(action.value)
        }
    }

    @Composable
    override fun Content(state: StockEmitterState, slots: ComposeMenuSlots<NetStockEmitterMenu>) {
        val colors = Bd.colors
        val page = rememberPageSelection()
        val settingsOpen = page.settings
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
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
                    BdSectionLabel(text.watched)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.bdSlotFrame(colors)) { BdSlot(slots, markSlot) }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            OreText(
                                state.mark ?: text.unmarked,
                                color = colors[if (state.mark != null) BdColors.text else BdColors.faint],
                                maxLines = 1,
                            )
                            OreText(state.amount, color = colors[BdColors.muted], style = Bd.caption, maxLines = 1)
                            Signal(state.emitting, text)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    BdSectionLabel(text.condition)
                    Spacer(Modifier.height(4.dp))
                    state.condition?.let {
                        BdMachineSettings(
                            listOf(it),
                            page.main
                        ) { pick -> send(StockEmitterAction.Setting(pick)) }
                    }
                    Spacer(Modifier.height(5.dp))
                    BdSettingRow(text.threshold) {
                        BdNumberEditor(
                            state.threshold,
                            { send(StockEmitterAction.Threshold(it)) },
                            0L..Long.MAX_VALUE,
                            editable
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdMachineSettings(state.settings, settingsOpen) { send(StockEmitterAction.Setting(it)) }
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

/** [mark] 为标记资源的名称，没有标记时为 null；[condition] 是主页面上的发信条件 */
data class StockEmitterState(
    val mark: String?,
    val amount: String,
    val threshold: Long,
    val emitting: Boolean,
    val condition: MachineSettingRow?,
    val settings: List<MachineSettingRow>,
)

sealed interface StockEmitterAction {
    data class Setting(val pick: MachineSettingPick) : StockEmitterAction

    data class Threshold(val value: Long) : StockEmitterAction
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
