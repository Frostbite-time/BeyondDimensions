package com.wintercogs.beyonddimensions.client.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
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
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import kotlin.math.abs


class NetEnergyScreen(
    menu: NetEnergyMenu,
    inventory: Inventory,
    title: Component
) : BdInventoryScreen<NetEnergyMenu, NetEnergyState, NetEnergyAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get()))
    private val text = NetEnergyText(title.string)

    override fun snapshot() = NetEnergyState(
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
    val screen = LocalBdScreen.current
    val panelReveal = remember { Animatable(0f) }
    val contentOpacity = remember { Animatable(0f) }
    var entranceComplete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        panelReveal.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        contentOpacity.animateTo(1f, tween(durationMillis = 240, easing = LinearOutSlowInEasing))
        entranceComplete = true
    }
    slots.Interaction(enabled = entranceComplete)
    var settingsExpanded by remember { mutableStateOf(false) }
    val settingsWidth by animateDpAsState(
        targetValue = if (settingsExpanded) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "Energy settings width",
    )

    Box(
        Modifier.fillMaxSize().background(Color(0x400A1423)),
        contentAlignment = Alignment.Center,
    ) {
        BdWindow(
            // 实际布局始终居中；设置区变宽时，主体和槽位一起向左移动。
            Modifier.width(181.dp + settingsWidth)
                .graphicsLayer {
                    scaleY = panelReveal.value
                    transformOrigin = TransformOrigin.Center
                }
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            colors.cyan.copy(alpha = 1f - contentOpacity.value),
                            colors.violet.copy(alpha = 1f - contentOpacity.value),
                        )
                    ),
                    Bd.WindowShape,
                )
                .pointerInput(entranceComplete) {
                    if (!entranceComplete) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
                }
                .then(slots.areaModifier())
        ) {
            // 展开阶段先显示框架，内容只在面板恢复原尺寸后淡入。
            Column(Modifier.graphicsLayer { alpha = contentOpacity.value }) {
                BdHeader(
                    icon = icon,
                    overline = "BEYOND DIMENSIONS",
                    title = text.title,
                    status = null,
                    onClose = screen::close,
                    actions = {
                        BdGlyphButton(
                            OreGlyph.Gear,
                            text.settings,
                            { settingsExpanded = !settingsExpanded },
                            selected = settingsExpanded,
                            size = 13.dp,
                            glyphSize = 7.dp,
                        )
                    },
                )

                Row(
                    Modifier.padding(
                        start = 8.dp,
                        end = 8.dp,
                        top = 7.dp,
                        bottom = 8.dp,
                    )
                ) {
                    // 左侧：能量读数和玩家背包。
                    Column(Modifier.width(163.dp)) {
                        BdSectionLabel(text.network) {
                            OreText(
                                "FE",
                                color = colors.faint,
                                style = Bd.caption,
                            )
                        }

                        Spacer(Modifier.height(5.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            OreText(
                                formatReadout(state.energyStored),
                                color = colors.text,
                                style = Bd.title,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(3.dp))
                            OreText(
                                "/ " + formatReadout(state.energyCapacity),
                                color = colors.muted,
                                maxLines = 1,
                            )
                            Spacer(Modifier.weight(1f))

                            val rateColor =
                                if (state.energyRate >= 0) colors.online
                                else colors.danger

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OreIcon(
                                    if (state.energyRate >= 0) OreGlyph.ArrowUp
                                    else OreGlyph.ArrowDown,
                                    Modifier.size(6.dp),
                                    color = rateColor,
                                )
                                Spacer(Modifier.width(2.dp))
                                OreText(
                                    formatReadout(abs(state.energyRate)) + " FE/t",
                                    color = rateColor,
                                    style = Bd.caption,
                                    maxLines = 1,
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        val fraction =
                            if (state.energyCapacity > 0) {
                                (
                                        state.energyStored.toDouble() /
                                                state.energyCapacity
                                        ).toFloat()
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
                                OreText(
                                    tick,
                                    color = colors.faint,
                                    style = Bd.caption,
                                    maxLines = 1,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        BdSectionLabel(text.inventory)
                        Spacer(Modifier.height(4.dp))

                        Box(
                            Modifier.background(colors.line).padding(0.5.dp)
                        ) {
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

                    NetEnergySettings(settingsExpanded, settingsWidth, state, text, onAction)
                }
            }
        }
    }
}

@Composable
private fun NetEnergySettings(
    expanded: Boolean,
    width: Dp,
    state: NetEnergyState,
    text: NetEnergyText,
    onAction: (NetEnergyAction) -> Unit,
) {
    val colors = Bd.colors
    // 设置保持完整宽度，从顶部展开；外层只露出窗口已展开的部分。
    Box(Modifier.width(width).clipToBounds()) {
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
            enter = expandVertically(
                animationSpec = tween(durationMillis = 240, delayMillis = 80, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ) + fadeIn(tween(durationMillis = 180, delayMillis = 80)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(tween(durationMillis = 120)),
        ) {
            Column(
                Modifier.padding(start = 10.dp).width(140.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BdSectionLabel(text.settings)

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        OreText(
                            text.pop,
                            color = colors.text,
                            maxLines = 1,
                        )
                        OreText(
                            text.popHint,
                            color = colors.faint,
                            style = Bd.caption,
                            maxLines = 1,
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    BdToggle(
                        checked = state.popMode == PopMode.OPEN,
                        onCheckedChange = { checked ->
                            onAction(
                                NetEnergyAction.SetPopMode(
                                    if (checked) PopMode.OPEN
                                    else PopMode.STOP
                                )
                            )
                        },
                    )
                }

                Column(Modifier.fillMaxWidth()) {
                    OreText(
                        text.redstone,
                        color = colors.text,
                        maxLines = 1,
                    )

                    Spacer(Modifier.height(2.dp))

                    BdSegmented(
                        options = text.redstoneOptions,
                        selected = state.redstoneMode.ordinal,
                        onSelect = { index ->
                            onAction(
                                NetEnergyAction.SetRedstoneMode(
                                    RedStoneControlMode.entries[index]
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fill = true,
                    )
                }
            }
        }
    }
}

data class NetEnergyState(
    val popMode: PopMode,
    val redstoneMode: RedStoneControlMode,
    val energyStored: Long,
    val energyCapacity: Long,
    val energyRate: Long
)

sealed interface NetEnergyAction {
    data class SetPopMode(val popMode: PopMode) : NetEnergyAction
    data class SetRedstoneMode(val redstoneMode: RedStoneControlMode) : NetEnergyAction
}

private class NetEnergyText(val title: String) {
    val syncing = tr("ui.beyonddimensions.status.syncing")
    val network = tr("ui.beyonddimensions.energy.network")
    val inventory = tr("ui.beyonddimensions.inventory")
    val settings = tr("ui.beyonddimensions.machine.settings")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions = RedStoneControlMode.entries.map {
        tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}")
    }
}
