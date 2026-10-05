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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
import com.wintercogs.beyonddimensions.client.ui.base.LocalBdScreen
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_PITCH
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
import dev.compixel.ui.ore.button.OreButtonStyle
import dev.compixel.ui.ore.button.OreIconButton
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreProgressBar
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.layout.OreSurface
import dev.compixel.ui.ore.selection.OreSwitch
import dev.compixel.ui.ore.selection.OreTabButton
import dev.compixel.ui.ore.theme.OreTheme
import dev.compixel.ui.theme.ThemeId
import kotlin.math.abs
import kotlin.math.roundToInt
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

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
            style = style,
        )

    override fun handle(action: NetEnergyAction) {
        when (action) {
            is NetEnergyAction.SetRedstoneMode -> container.requestRedstone(action.redstoneMode.ordinal)
            is NetEnergyAction.SetPopMode -> container.requestOutput(action.popMode.ordinal)
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // 临时：按 F6 在两种风格之间切换，便于在游戏里对比；定下风格后删除
        if (keyCode == GLFW.GLFW_KEY_F6 && !hasTextInputFocus) {
            style = if (style == NetEnergyStyle.Tech) NetEnergyStyle.Vanilla else NetEnergyStyle.Tech
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    @Composable
    override fun Page(state: NetEnergyState, slots: ComposeMenuSlots<NetEnergyMenu>) {
        val screen = LocalBdScreen.current
        when (state.style) {
            NetEnergyStyle.Tech -> NetEnergyTech(slots, state, icon, text, ::send, screen::close)
            NetEnergyStyle.Vanilla -> NetEnergyVanilla(slots, state, text, ::send, screen::close)
        }
    }

    private companion object {
        // 对比期间记住上次的选择，重开界面不必再按 F6
        var style = NetEnergyStyle.Tech
    }
}

/**
 * 两种风格共用的动画参数，方便在游戏里调手感。参照基岩版的容器界面：打开时从屏幕下方先快后慢地滑上来并淡入，
 * 背景同时变暗；关闭时先慢后快地滑下去并淡出。关闭时玩家立即拿回操作，退场由 CompixelUI 在游戏画面上播完。
 */
private const val OPEN_MILLIS = 180
private const val EXIT_MILLIS = 180
private const val RISE_OF_SCREEN = 0.25f
private const val SETTINGS_MILLIS = 150
private const val SETTINGS_RISE_DP = 6

/**
 * 两种风格共用的界面骨架：变暗的背景与居中的窗口一起淡入淡出，窗口同时从下方升起、退场时滑回下方，距离是屏幕高度的
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
// 风格一：当前的冰白科技风

@Composable
private fun NetEnergyTech(
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

    NetEnergyFrame(Color(0x400A1423), 181.dp) {
        BdWindow(
            Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - 0.6f * settings.value }.then(slots.areaModifier())
        ) {
            TechHeader(icon, text.title, settingsPage = false, !settingsOpen, text.settings, { settingsOpen = true }, onClose)
            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
                TechSection(text.network) {
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
                TechSection(text.inventory)
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
                TechHeader(icon, text.settings, settingsPage = true, settingsOpen, text.back, { settingsOpen = false }, onClose)
                TechSettings(state, text, settingsOpen, onAction)
            }
        }
    }
}

@Composable
private fun TechSettings(
    state: NetEnergyState,
    text: NetEnergyText,
    enabled: Boolean,
    onAction: (NetEnergyAction) -> Unit,
) {
    val colors = Bd.colors
    Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TechSection(text.settings)

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

@Composable
private fun TechHeader(
    icon: ItemIcon,
    title: String,
    settingsPage: Boolean,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = Bd.colors
    Box(Modifier.fillMaxWidth().height(2.dp).background(colors.signature))
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(colors.surface).padding(start = 7.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MinecraftItemIcon(icon, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            OreText("BEYOND DIMENSIONS", color = colors.faint, style = Bd.overline, maxLines = 1)
            OreText(title, color = colors.text, style = Bd.body, maxLines = 1)
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

@Composable
private fun TechSection(text: String, trailing: (@Composable () -> Unit)? = null) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth().height(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(2.dp).background(colors.accent))
        Spacer(Modifier.width(3.dp))
        OreText(text, color = colors.muted, style = Bd.overline, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.line))
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 风格二：Ore 组件配原版颜色。颜色来自 compixel/themes/vanilla.json 与 vanilla_energy.json，可以改文件后按 F3+T 重载

private val VanillaTheme = ThemeId("beyonddimensions", "vanilla")
private val VanillaEnergyTheme = ThemeId("beyonddimensions", "vanilla_energy")
private val VanillaGain = Color(0xFF2A7A2A)
private val VanillaLoss = Color(0xFFA02020)

@Composable
private fun NetEnergyVanilla(
    slots: ComposeMenuSlots<NetEnergyMenu>,
    state: NetEnergyState,
    text: NetEnergyText,
    onAction: (NetEnergyAction) -> Unit,
    onClose: () -> Unit,
) = OreTheme(VanillaTheme) {
    val colors = OreTheme.colors
    var settingsOpen by remember { mutableStateOf(false) }
    val settings = rememberSettingsMotion(settingsOpen)
    slots.Interaction(enabled = !settingsOpen)

    // 9 个槽位加两侧 5 像素内边距，再加 Ore 面板两侧各 2 像素的边框
    NetEnergyFrame(colors.backdrop, 176.dp) {
        OreWindowFrame(
            text.title,
            Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - 0.6f * settings.value }.then(slots.areaModifier()),
            leading = {
                OreIconButton(
                    OreGlyph.Gear,
                    text.settings,
                    { settingsOpen = true },
                    enabled = !settingsOpen,
                    style = OreButtonStyle.Quiet,
                )
            },
            trailing = {
                OreIconButton(OreGlyph.Cross, text.close, onClose, enabled = !settingsOpen, style = OreButtonStyle.Quiet)
            },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OreText(text.network, Modifier.weight(1f), color = colors.text, maxLines = 1)
                OreText("FE", color = colors.mutedText, style = OreTheme.typography.caption)
            }

            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                OreText(formatReadout(state.energyStored), color = colors.text, style = OreTheme.typography.title, maxLines = 1)
                Spacer(Modifier.width(3.dp))
                OreText("/ " + formatReadout(state.energyCapacity), color = colors.mutedText, maxLines = 1)
                Spacer(Modifier.weight(1f))
                val rateColor = if (state.energyRate >= 0) VanillaGain else VanillaLoss
                OreIcon(
                    if (state.energyRate >= 0) OreGlyph.ArrowUp else OreGlyph.ArrowDown,
                    Modifier.size(6.dp),
                    color = rateColor,
                )
                Spacer(Modifier.width(2.dp))
                OreText(formatReadout(abs(state.energyRate)) + " FE/t", color = rateColor, maxLines = 1)
            }

            Spacer(Modifier.height(3.dp))
            OreTheme(VanillaEnergyTheme) { OreProgressBar(state.fraction, Modifier.height(10.dp)) }
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (tick in TICKS) OreText(tick, color = colors.mutedText, style = OreTheme.typography.caption, maxLines = 1)
            }

            Spacer(Modifier.height(6.dp))
            OreText(text.inventory, color = colors.text, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            // 菜单槽位 0～26 是背包三行，27～35 是快捷栏；间距与原版相同
            repeat(3) { row ->
                Row { repeat(9) { column -> slots.Slot(row * 9 + column, Modifier.size(SLOT_PITCH.dp)) } }
            }
            Spacer(Modifier.height(4.dp))
            Row { repeat(9) { column -> slots.Slot(27 + column, Modifier.size(SLOT_PITCH.dp)) } }
        }

        if (settingsOpen || settings.value > 0f) {
            OreWindowFrame(
                text.settings,
                Modifier.matchParentSize().rise(settings, SETTINGS_RISE_DP),
                leading = {
                    OreIconButton(
                        OreGlyph.ArrowLeft,
                        text.back,
                        { settingsOpen = false },
                        enabled = settingsOpen,
                        style = OreButtonStyle.Quiet,
                    )
                },
                trailing = {
                    OreIconButton(OreGlyph.Cross, text.close, onClose, enabled = settingsOpen, style = OreButtonStyle.Quiet)
                },
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        OreText(text.pop, color = colors.text, maxLines = 1)
                        OreText(text.popHint, color = colors.mutedText, style = OreTheme.typography.caption, maxLines = 1)
                    }
                    Spacer(Modifier.width(4.dp))
                    OreSwitch(
                        checked = state.popMode == PopMode.OPEN,
                        onCheckedChange = { checked ->
                            onAction(NetEnergyAction.SetPopMode(if (checked) PopMode.OPEN else PopMode.STOP))
                        },
                        enabled = settingsOpen,
                    )
                }

                Spacer(Modifier.height(8.dp))
                OreText(text.redstone, color = colors.text, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                OreTabButton(
                    options = text.redstoneOptions,
                    selectedIndex = state.redstoneMode.ordinal,
                    onSelectionChange = { index ->
                        onAction(NetEnergyAction.SetRedstoneMode(RedStoneControlMode.entries[index]))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = settingsOpen,
                )
            }
        }
    }
}

/**
 * 与 OrePanel 相同的面板外观：Ore 边框、凸起的标题栏与分隔线，标题居中，左右各一个按钮位。
 * OrePanel 自身会撑满可用高度，这里按内容决定高度。
 */
@Composable
private fun OreWindowFrame(
    title: String,
    modifier: Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OreTheme.colors
    OreSurface(modifier, bottomLedge = 2.dp, ledgeColor = colors.ledge, frameEdge = colors.frameEdge) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth()
                    .background(colors.raised)
                    .heightIn(min = 20.dp)
                    .padding(horizontal = 5.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                leading()
                OreText(
                    title,
                    Modifier.weight(1f),
                    style = OreTheme.typography.title,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                trailing()
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.ledge))
            Column(Modifier.fillMaxWidth().padding(start = 5.dp, top = 5.dp, end = 5.dp, bottom = 6.dp), content = content)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

private val TICKS = listOf("0", "25", "50", "75", "100%")

/** 临时的风格对比开关 */
enum class NetEnergyStyle {
    Tech,
    Vanilla,
}

data class NetEnergyState(
    val popMode: PopMode,
    val redstoneMode: RedStoneControlMode,
    val energyStored: Long,
    val energyCapacity: Long,
    val energyRate: Long,
    val style: NetEnergyStyle = NetEnergyStyle.Tech,
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
    val close = tr("ui.beyonddimensions.close")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}
