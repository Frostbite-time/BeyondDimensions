package com.wintercogs.beyonddimensions.client.screen

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
import com.wintercogs.beyonddimensions.client.ui.base.LocalBdScreen
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.ScreenTransition
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.theme.OreTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class NetEnergyScreen(
    menu: NetEnergyMenu,
    inventory: Inventory,
    title: Component,
) : BdInventoryScreen<NetEnergyMenu, NetEnergyState, NetEnergyAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get()))
    private val text = NetEnergyText(title.string)

    override fun snapshot() =
        NetEnergyState(
            popMode = container.popMode(),
            redstoneMode = container.redStoneMode(),
            energyStored = container.energyStored(),
            energyCapacity = container.energyCapacity(),
            energyRate = container.energyRate(),
        )

    override fun handle(action: NetEnergyAction) {
        when (action) {
            is NetEnergyAction.SetRedstoneMode -> container.requestRedstone(action.redstoneMode.ordinal)
            is NetEnergyAction.SetPopMode -> container.requestOutput(action.popMode.ordinal)
        }
    }

    @Composable
    override fun Page(state: NetEnergyState, slots: ComposeMenuSlots<NetEnergyMenu>) {
        val screen = LocalBdScreen.current
        NetEnergyView(slots, state, icon, text, ::send, screen::close)
    }
}

/**
 * 动画参数，方便在游戏里调手感。参照基岩版的容器界面：打开时从屏幕下方先快后慢地滑上来并淡入，背景同时变暗；
 * 关闭时先慢后快地滑下去并淡出。关闭时玩家立即拿回操作，退场由 CompixelUI 在游戏画面上播完。
 */
private const val OPEN_MILLIS = 180
private const val EXIT_MILLIS = 180
private const val RISE_OF_SCREEN = 0.25f
private const val SETTINGS_MILLIS = 150
private const val SETTINGS_RISE_DP = 6

/**
 * 界面骨架：变暗的背景与居中的窗口一起淡入淡出，窗口同时从下方升起、退场时滑回下方，距离是屏幕高度的
 * [RISE_OF_SCREEN]。滑动在布局阶段移动窗口，原生槽位的命中区域随之更新，动画中途也能点准。
 */
@Composable
private fun NetEnergyFrame(backdrop: Color, width: Dp, window: @Composable BoxScope.() -> Unit) =
    ScreenTransition(
        enter = fadeIn(tween(OPEN_MILLIS, easing = LinearOutSlowInEasing)),
        exit = fadeOut(tween(EXIT_MILLIS, easing = LinearEasing)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val rise = (constraints.maxHeight * RISE_OF_SCREEN).roundToInt()
            Box(Modifier.matchParentSize().background(backdrop))
            Box(
                Modifier.width(width).animateEnterExit(
                    enter = slideInVertically(tween(OPEN_MILLIS, easing = LinearOutSlowInEasing)) { rise },
                    exit = slideOutVertically(tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) { rise },
                ),
                content = window,
            )
        }
    }

/** 页面内的小幅过渡，例如设置页 */
private fun Modifier.rise(progress: State<Float>, distanceDp: Int): Modifier =
    offset { IntOffset(0, ((1f - progress.value) * distanceDp.dp.toPx()).roundToInt()) }
        .graphicsLayer { alpha = progress.value }

@Composable
private fun rememberSettingsMotion(open: Boolean): State<Float> =
    animateFloatAsState(
        if (open) 1f else 0f,
        tween(SETTINGS_MILLIS, easing = FastOutSlowInEasing),
        label = "settings",
    )

// ---------------------------------------------------------------------------------------------------------------------

@Composable
private fun NetEnergyView(
    slots: ComposeMenuSlots<NetEnergyMenu>,
    state: NetEnergyState,
    icon: ItemIcon,
    text: NetEnergyText,
    onAction: (NetEnergyAction) -> Unit,
    onClose: () -> Unit,
) {
    val colors = Bd.colors
    var settingsOpen by remember { mutableStateOf(false) }
    val settings = rememberSettingsMotion(settingsOpen)
    slots.Interaction(enabled = !settingsOpen)

    NetEnergyFrame(OreTheme.colors.backdrop, 181.dp) {
        // 窗口半透明，设置页打开时主页完全淡出，免得两层内容透在一起
        BdWindow(Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - settings.value }.then(slots.areaModifier())) {
            EnergyHeader(icon, text.title, settingsPage = false, !settingsOpen, text.settings, { settingsOpen = true }, onClose)
            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
                BdSectionLabel(text.network) {
                    OreText("FE", color = colors.faint, style = Bd.caption)
                }

                Spacer(Modifier.height(5.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    OreText(formatReadout(state.energyStored), color = colors.text, style = Bd.title, maxLines = 1)
                    Spacer(Modifier.width(3.dp))
                    OreText("/ " + formatReadout(state.energyCapacity), color = colors.muted, style = Bd.body, maxLines = 1)
                    Spacer(Modifier.weight(1f))

                    val rateColor = if (state.energyRate >= 0) colors.online else colors.danger
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OreIcon(
                            if (state.energyRate >= 0) OreGlyph.ArrowUp else OreGlyph.ArrowDown,
                            Modifier.size(6.dp),
                            color = rateColor,
                        )
                        Spacer(Modifier.width(2.dp))
                        OreText(formatReadout(abs(state.energyRate)) + " FE/t", color = rateColor, style = Bd.caption, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(4.dp))
                BdMeter(state.fraction, Modifier.fillMaxWidth().height(11.dp))
                Spacer(Modifier.height(2.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (tick in TICKS) OreText(tick, color = colors.faint, style = Bd.caption, maxLines = 1)
                }

                Spacer(Modifier.height(8.dp))
                BdSectionLabel(text.inventory)
                Spacer(Modifier.height(4.dp))

                Box(Modifier.background(colors.line).padding(0.5.dp)) {
                    Column {
                        // 菜单槽位 0～26：背包三行。
                        repeat(3) { row -> Row { repeat(9) { column -> BdSlot(slots, row * 9 + column) } } }
                        Spacer(Modifier.height(3.dp))
                        // 菜单槽位 27～35：快捷栏。
                        Row { repeat(9) { column -> BdSlot(slots, 27 + column) } }
                    }
                }
            }
        }
        if (settingsOpen || settings.value > 0f) {
            BdWindow(Modifier.matchParentSize().rise(settings, SETTINGS_RISE_DP)) {
                EnergyHeader(icon, text.settings, settingsPage = true, settingsOpen, text.back, { settingsOpen = false }, onClose)
                EnergySettings(state, text, settingsOpen, onAction)
            }
        }
    }
}

@Composable
private fun EnergySettings(
    state: NetEnergyState,
    text: NetEnergyText,
    enabled: Boolean,
    onAction: (NetEnergyAction) -> Unit,
) {
    val colors = Bd.colors
    Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BdSectionLabel(text.settings)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                OreText(text.pop, color = colors.text, style = Bd.body, maxLines = 1)
                OreText(text.popHint, color = colors.faint, style = Bd.caption, maxLines = 1)
            }
            Spacer(Modifier.width(4.dp))
            BdToggle(
                checked = state.popMode == PopMode.OPEN,
                enabled = enabled,
                onCheckedChange = { checked ->
                    onAction(NetEnergyAction.SetPopMode(if (checked) PopMode.OPEN else PopMode.STOP))
                },
            )
        }

        Column(Modifier.fillMaxWidth()) {
            OreText(text.redstone, color = colors.text, style = Bd.body, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            BdSegmented(
                options = text.redstoneOptions,
                selected = state.redstoneMode.ordinal,
                onSelect = { index -> onAction(NetEnergyAction.SetRedstoneMode(RedStoneControlMode.entries[index])) },
                modifier = Modifier.fillMaxWidth(),
                fill = true,
                enabled = enabled,
            )
        }
    }
}

/** 标题栏：设备图标、模组名与标题，右侧是设置（或返回）与关闭；被设置页盖住的一层不响应点击 */
@Composable
private fun EnergyHeader(
    icon: ItemIcon,
    title: String,
    settingsPage: Boolean,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = Bd.colors
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(colors.surface).padding(start = 7.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MinecraftItemIcon(icon, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            OreText("BEYOND DIMENSIONS", color = colors.faint, style = Bd.overline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            OreText(title, color = colors.text, style = Bd.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(4.dp))
        BdGlyphButton(
            if (settingsPage) OreGlyph.ArrowLeft else OreGlyph.Gear,
            actionLabel,
            onAction,
            enabled = enabled,
            size = 13.dp,
            glyphSize = 7.dp,
        )
        Spacer(Modifier.width(4.dp))
        BdGlyphButton(OreGlyph.Cross, null, onClose, enabled = enabled, size = 13.dp, glyphSize = 7.dp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

// ---------------------------------------------------------------------------------------------------------------------

private val TICKS = listOf("0", "25", "50", "75", "100%")

data class NetEnergyState(
    val popMode: PopMode,
    val redstoneMode: RedStoneControlMode,
    val energyStored: Long,
    val energyCapacity: Long,
    val energyRate: Long,
) {
    val fraction: Float
        get() = if (energyCapacity > 0) (energyStored.toDouble() / energyCapacity).toFloat() else 0f
}

sealed interface NetEnergyAction {
    data class SetPopMode(val popMode: PopMode) : NetEnergyAction

    data class SetRedstoneMode(val redstoneMode: RedStoneControlMode) : NetEnergyAction
}

private class NetEnergyText(val title: String) {
    val network = tr("ui.beyonddimensions.energy.network")
    val inventory = tr("ui.beyonddimensions.inventory")
    val settings = tr("ui.beyonddimensions.machine.settings")
    val back = tr("gui.back")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}
