package com.wintercogs.beyonddimensions.client.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.mojang.blaze3d.platform.InputConstants
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
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
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

class NetEnergyScreen(
    menu: NetEnergyMenu,
    inventory: Inventory,
    title: Component,
) : BdInventoryScreen<NetEnergyMenu, NetEnergyState, NetEnergyAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get()))
    private val text = NetEnergyText(title.string)
    private var closePending = false

    override fun snapshot() =
        NetEnergyState(
            popMode = container.popMode(),
            redstoneMode = container.redStoneMode(),
            energyStored = container.energyStored(),
            energyCapacity = container.energyCapacity(),
            energyRate = container.energyRate(),
            closing = closePending,
        )

    override fun handle(action: NetEnergyAction) {
        when (action) {
            NetEnergyAction.RequestClose -> onClose()
            NetEnergyAction.FinishClose -> super.onClose()
            is NetEnergyAction.SetRedstoneMode ->
                if (!closePending) container.requestRedstone(action.redstoneMode.ordinal)
            is NetEnergyAction.SetPopMode ->
                if (!closePending) container.requestOutput(action.popMode.ordinal)
        }
    }

    // 原版的 Escape、背包键和标题栏关闭都先发布关闭快照，动画结束后再关闭菜单。
    override fun onClose() {
        closePending = true
    }

    override fun executeInventoryClick(slotId: Int, button: Int, type: ClickType) {
        if (!closePending) super.executeInventoryClick(slotId, button, type)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (
            !hasTextInputFocus &&
                (keyCode == GLFW.GLFW_KEY_ESCAPE ||
                    minecraft!!
                        .options
                        .keyInventory
                        .isActiveAndMatches(InputConstants.getKey(keyCode, scanCode)))
        ) {
            onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun removed() {
        closePending = false
        super.removed()
    }

    @Composable
    override fun Page(state: NetEnergyState, slots: ComposeMenuSlots<NetEnergyMenu>) =
        NetEnergyContent(slots, state, icon, text, ::send)
}

@Composable
private fun NetEnergyContent(
    slots: ComposeMenuSlots<NetEnergyMenu>,
    state: NetEnergyState,
    icon: ItemIcon,
    text: NetEnergyText,
    onAction: (NetEnergyAction) -> Unit,
) {
    val colors = Bd.colors
    var settingsExpanded by remember { mutableStateOf(false) }
    val settingsProgress = remember { Animatable(0f) }
    val settingsDecode = remember { Animatable(1f) }
    val cardEasing = remember { CubicBezierEasing(0.7f, 0f, 0.2f, 1f) }
    LaunchedEffect(settingsExpanded) {
        if (settingsExpanded) {
            settingsDecode.snapTo(0f)
            coroutineScope {
                launch { settingsProgress.animateTo(1f, tween(580, easing = cardEasing)) }
                launch {
                    delay(260)
                    settingsDecode.animateTo(1f, tween(580, easing = LinearEasing))
                }
            }
        } else {
            settingsProgress.animateTo(0f, tween(480, easing = cardEasing))
            settingsDecode.snapTo(1f)
        }
    }
    LaunchedEffect(state.closing) {
        if (state.closing) settingsExpanded = false
    }
    val motion =
        rememberBdTerminalMotion(
            closing = state.closing && settingsProgress.value == 0f,
            onClosed = { onAction(NetEnergyAction.FinishClose) },
        )
    val settingsMoving =
        settingsProgress.isRunning ||
            settingsDecode.isRunning ||
            settingsProgress.value != if (settingsExpanded) 1f else 0f
    val interactive = motion.ready && !state.closing && !settingsMoving
    slots.Interaction(enabled = interactive && !settingsExpanded)
    val close = { onAction(NetEnergyAction.RequestClose) }

    Box(
        Modifier.fillMaxSize().background(Color(0x400A1423)),
        contentAlignment = Alignment.Center,
    ) {
        BdTerminalSurface(motion, interactive, Modifier.width(181.dp)) {
            BdWindow(
                Modifier.fillMaxWidth()
                    .graphicsLayer {
                        val p = settingsProgress.value
                        scaleX = 1f - 0.07f * p
                        scaleY = 1f - 0.07f * p
                        translationY = -16.dp.toPx() * p
                        alpha = 1f - 0.58f * p
                    }
                    .then(slots.areaModifier())
            ) {
                NetEnergyHeader(
                    icon = icon,
                    title = text.title,
                    progress = motion.decode.value,
                    settings = false,
                    enabled = interactive && !settingsExpanded,
                    actionLabel = text.settings,
                    onSettings = { settingsExpanded = true },
                    onClose = close,
                )
                Column(
                    Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 7.dp,
                        bottom = 8.dp,
                    )
                ) {
                    NetEnergySection(text.network, motion.decode.value, 1) {
                        OreText(
                            "FE",
                            color = colors.faint,
                            style = Bd.caption,
                        )
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        BdTerminalText(
                            formatReadout(state.energyStored),
                            motion.decode.value,
                            order = 2,
                            color = colors.text,
                            style = Bd.title,
                        )
                        Spacer(Modifier.width(3.dp))
                        BdTerminalText(
                            "/ " + formatReadout(state.energyCapacity),
                            motion.decode.value,
                            order = 2,
                            color = colors.muted,
                        )
                        Spacer(Modifier.weight(1f))

                        val rateColor = if (state.energyRate >= 0) colors.online else colors.danger

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OreIcon(
                                if (state.energyRate >= 0) OreGlyph.ArrowUp else OreGlyph.ArrowDown,
                                Modifier.size(6.dp),
                                color = rateColor,
                            )
                            Spacer(Modifier.width(2.dp))
                            BdTerminalText(
                                formatReadout(abs(state.energyRate)) + " FE/t",
                                motion.decode.value,
                                order = 3,
                                color = rateColor,
                                style = Bd.caption,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    val fraction =
                        if (state.energyCapacity > 0) {
                            (state.energyStored.toDouble() / state.energyCapacity).toFloat()
                        } else {
                            0f
                        }

                    BdMeter(
                        fraction,
                        Modifier.fillMaxWidth().height(11.dp),
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (tick in listOf("0", "25", "50", "75", "100%")) {
                            BdTerminalText(
                                tick,
                                motion.decode.value,
                                order = 4,
                                color = colors.faint,
                                style = Bd.caption,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    NetEnergySection(text.inventory, motion.decode.value, 5)
                    Spacer(Modifier.height(4.dp))

                    Box(Modifier.background(colors.line).padding(0.5.dp)) {
                        Column {
                            // 菜单槽位 0～26：背包三行。
                            repeat(3) { row ->
                                Row {
                                    repeat(9) { column ->
                                        BdSlot(slots, row * 9 + column)
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))

                            // 菜单槽位 27～35：快捷栏。
                            Row {
                                repeat(9) { column ->
                                    BdSlot(slots, 27 + column)
                                }
                            }
                        }
                    }
                }
            }
            if (settingsExpanded || settingsProgress.value > 0f) {
                BdWindow(
                    Modifier.matchParentSize().graphicsLayer {
                        val p = ((settingsProgress.value - 0.1f) / 0.9f).coerceIn(0f, 1f)
                        translationY = 9.dp.toPx() * (1f - p)
                        scaleX = 1f + 0.05f * (1f - p)
                        scaleY = scaleX
                        alpha = p
                    }
                ) {
                    NetEnergyHeader(
                        icon,
                        text.settings,
                        settingsDecode.value,
                        true,
                        interactive,
                        text.back,
                        onSettings = { settingsExpanded = false },
                        onClose = close,
                    )
                    NetEnergySettings(state, text, settingsDecode.value, interactive, onAction)
                }
            }
        }
    }
}

@Composable
private fun NetEnergySettings(
    state: NetEnergyState,
    text: NetEnergyText,
    progress: Float,
    enabled: Boolean,
    onAction: (NetEnergyAction) -> Unit,
) {
    val colors = Bd.colors
    Column(
        Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NetEnergySection(text.settings, progress, 1)

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                BdTerminalText(
                    text.pop,
                    progress,
                    order = 2,
                    color = colors.text,
                )
                BdTerminalText(
                    text.popHint,
                    progress,
                    order = 3,
                    color = colors.faint,
                    style = Bd.caption,
                )
            }

            Spacer(Modifier.width(4.dp))

            BdToggle(
                checked = state.popMode == PopMode.OPEN,
                enabled = enabled,
                onCheckedChange = { checked ->
                    onAction(
                        NetEnergyAction.SetPopMode(if (checked) PopMode.OPEN else PopMode.STOP)
                    )
                },
            )
        }

        Column(Modifier.fillMaxWidth()) {
            BdTerminalText(
                text.redstone,
                progress,
                order = 4,
                color = colors.text,
            )

            Spacer(Modifier.height(2.dp))

            BdSegmented(
                options = text.redstoneOptions,
                selected = state.redstoneMode.ordinal,
                onSelect = { index ->
                    onAction(NetEnergyAction.SetRedstoneMode(RedStoneControlMode.entries[index]))
                },
                modifier = Modifier.fillMaxWidth(),
                fill = true,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun NetEnergyHeader(
    icon: ItemIcon,
    title: String,
    progress: Float,
    settings: Boolean,
    enabled: Boolean,
    actionLabel: String,
    onSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = Bd.colors
    Box(Modifier.fillMaxWidth().height(2.dp).background(colors.signature))
    Row(
        Modifier.fillMaxWidth()
            .height(24.dp)
            .background(colors.surface)
            .padding(start = 7.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MinecraftItemIcon(icon, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            BdTerminalText("BEYOND DIMENSIONS", progress, color = colors.faint, style = Bd.overline)
            BdTerminalText(title, progress, color = colors.text)
        }
        Spacer(Modifier.width(4.dp))
        BdGlyphButton(
            if (settings) OreGlyph.ArrowLeft else OreGlyph.Gear,
            actionLabel,
            onSettings,
            enabled = enabled,
            size = 13.dp,
            glyphSize = 7.dp,
        )
        Spacer(Modifier.width(4.dp))
        BdGlyphButton(
            OreGlyph.Cross,
            null,
            onClose,
            enabled = enabled,
            size = 13.dp,
            glyphSize = 7.dp,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

@Composable
private fun NetEnergySection(
    text: String,
    progress: Float,
    order: Int,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth().height(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(2.dp).background(colors.accent))
        Spacer(Modifier.width(3.dp))
        BdTerminalText(text, progress, order, color = colors.muted, style = Bd.overline)
        Spacer(Modifier.width(4.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.line))
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

data class NetEnergyState(
    val popMode: PopMode,
    val redstoneMode: RedStoneControlMode,
    val energyStored: Long,
    val energyCapacity: Long,
    val energyRate: Long,
    val closing: Boolean = false,
)

sealed interface NetEnergyAction {
    data object RequestClose : NetEnergyAction

    data object FinishClose : NetEnergyAction

    data class SetPopMode(val popMode: PopMode) : NetEnergyAction

    data class SetRedstoneMode(val redstoneMode: RedStoneControlMode) : NetEnergyAction
}

private class NetEnergyText(val title: String) {
    val syncing = tr("ui.beyonddimensions.status.syncing")
    val network = tr("ui.beyonddimensions.energy.network")
    val inventory = tr("ui.beyonddimensions.inventory")
    val settings = tr("ui.beyonddimensions.machine.settings")
    val back = tr("gui.back")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map {
            tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}")
        }
}
