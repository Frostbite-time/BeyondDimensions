package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import dev.compixel.ui.ore.display.OreText
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.pow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class BdTerminalPhase {
    Assemble,
    Reveal,
    Ready,
    Cover,
    Collapse,
    Closed,
}

/** 单个画布负责方格，不为每个格子创建图层；停稳后不再驱动动画帧。 */
@Stable
internal class BdTerminalMotion {
    var phase by mutableStateOf(BdTerminalPhase.Assemble)
    var grid by mutableStateOf<BdTerminalGrid?>(null)
    val elapsed = Animatable(0f)
    val decode = Animatable(0f)

    val ready
        get() = phase == BdTerminalPhase.Ready

    val panelVisible
        get() =
            phase == BdTerminalPhase.Reveal ||
                phase == BdTerminalPhase.Ready ||
                phase == BdTerminalPhase.Cover
}

internal data class BdTerminalGrid(val columns: Int, val rows: Int) {
    private val farCorner = hypot((columns - 1).toFloat(), (rows - 1).toFloat())
    val assembleDuration = ceil(farCorner / 2f * 24f + 180f).toInt()
    val revealDuration = (rows - 1) * 24 + (columns - 1) * 4 + 180
    val coverDuration = ceil(farCorner * 18f + 180f).toInt()
    val collapseDuration = ceil(farCorner * 20f + 180f).toInt()

    fun delay(phase: BdTerminalPhase, column: Int, row: Int): Float =
        when (phase) {
            BdTerminalPhase.Assemble ->
                hypot(column - (columns - 1) / 2f, row - (rows - 1) / 2f) * 24f
            BdTerminalPhase.Reveal -> row * 24f + column * 4f
            BdTerminalPhase.Cover -> hypot((columns - 1 - column).toFloat(), row.toFloat()) * 18f
            BdTerminalPhase.Collapse ->
                (farCorner - hypot((columns - 1 - column).toFloat(), row.toFloat())) * 20f
            else -> 0f
        }
}

@Composable
internal fun rememberBdTerminalMotion(closing: Boolean, onClosed: () -> Unit): BdTerminalMotion {
    val motion = remember { BdTerminalMotion() }
    val finishClose by rememberUpdatedState(onClosed)
    LaunchedEffect(motion.grid != null, closing) {
        val grid = motion.grid ?: return@LaunchedEffect
        suspend fun run(phase: BdTerminalPhase, duration: Int) {
            motion.elapsed.snapTo(0f)
            motion.phase = phase
            motion.elapsed.animateTo(duration.toFloat(), tween(duration, easing = LinearEasing))
        }
        if (closing) {
            run(BdTerminalPhase.Cover, grid.coverDuration)
            run(BdTerminalPhase.Collapse, grid.collapseDuration)
            motion.phase = BdTerminalPhase.Closed
            finishClose()
        } else if (motion.phase == BdTerminalPhase.Assemble) {
            run(BdTerminalPhase.Assemble, grid.assembleDuration)
            motion.elapsed.snapTo(0f)
            motion.phase = BdTerminalPhase.Reveal
            coroutineScope {
                launch {
                    motion.elapsed.animateTo(
                        grid.revealDuration.toFloat(),
                        tween(grid.revealDuration, easing = LinearEasing),
                    )
                }
                launch {
                    delay(100)
                    motion.decode.animateTo(1f, tween(580, easing = LinearEasing))
                }
            }
            motion.phase = BdTerminalPhase.Ready
        }
    }
    return motion
}

@Composable
internal fun BdTerminalSurface(
    motion: BdTerminalMotion,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = Bd.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val cellPixels = with(LocalDensity.current) { 13.dp.toPx() }
    Box(
        modifier
            .onSizeChanged { size: IntSize ->
                if (size.width > 0 && size.height > 0) {
                    motion.grid =
                        BdTerminalGrid(
                            ceil(size.width / cellPixels).toInt().coerceAtLeast(1),
                            ceil(size.height / cellPixels).toInt().coerceAtLeast(1),
                        )
                }
            }
            .pointerInput(interactive) {
                if (!interactive)
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach {
                            it.consume()
                        }
                    }
            }
    ) {
        // 始终测量真实内容，使方格、标题和原版槽位共享同一块固定区域。
        Box(Modifier.graphicsLayer { alpha = if (motion.panelVisible) 1f else 0f }) { content() }
        if (!motion.ready && motion.phase != BdTerminalPhase.Closed) {
            Canvas(Modifier.matchParentSize().clip(Bd.WindowShape)) {
                val grid = motion.grid ?: return@Canvas
                val phase = motion.phase
                val elapsed = motion.elapsed.value
                val cellWidth = size.width / grid.columns
                val cellHeight = size.height / grid.rows
                for (row in 0 until grid.rows) for (column in 0 until grid.columns) {
                    val logicalColumn = if (rtl) grid.columns - 1 - column else column
                    val p =
                        ((elapsed - grid.delay(phase, logicalColumn, row)) / 180f).coerceIn(0f, 1f)
                    val x = column * cellWidth
                    val y = row * cellHeight
                    val w = cellWidth + 0.6f
                    val h = cellHeight + 0.6f
                    when (phase) {
                        BdTerminalPhase.Assemble,
                        BdTerminalPhase.Cover -> {
                            if (p <= 0f) continue
                            val scale = 1f - (1f - p).pow(3)
                            drawRect(
                                if (p < 0.55f) colors.cyan else colors.text,
                                Offset(
                                    x + (cellWidth - w * scale) / 2f,
                                    y + (cellHeight - h * scale) / 2f,
                                ),
                                Size(w * scale, h * scale),
                            )
                        }
                        BdTerminalPhase.Reveal -> {
                            val height = h * (1f - terminalEase(p))
                            if (height > 0f) drawRect(colors.text, Offset(x, y), Size(w, height))
                        }
                        BdTerminalPhase.Collapse -> {
                            val scale = 1f - terminalEase(p)
                            if (scale > 0f)
                                drawRect(
                                    colors.text.copy(alpha = 0.6f + 0.4f * scale),
                                    Offset(if (rtl) x else x + w * (1f - scale), y),
                                    Size(w * scale, h * scale),
                                )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

private fun terminalEase(p: Float) =
    if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).pow(3) / 2f

/** 用最终文本测量，再覆盖乱码；解码时不改变布局和槽位坐标。 */
@Composable
internal fun BdTerminalText(
    text: String,
    progress: Float,
    order: Int = 0,
    modifier: Modifier = Modifier,
    color: Color = Bd.colors.text,
    style: TextStyle = Bd.body,
) {
    val p = ((progress * 580f - order * 34f) / 340f).coerceIn(0f, 1f)
    if (p >= 1f) {
        OreText(text, modifier, color = color, style = style, maxLines = 1)
    } else {
        val glyphs = "▚▞▘▝▗▖▙▟▛▜█▓▒░#/<>%"
        val characters =
            remember(text) { text.codePoints().toArray().map { String(Character.toChars(it)) } }
        val fixed = (p * characters.size).toInt()
        val frame = (progress * 580f / 32f).toInt()
        val scrambled =
            characters
                .mapIndexed { index, character ->
                    if (index < fixed || character.isBlank()) character
                    else glyphs[(index * 7 + frame * 11 + order * 3) % glyphs.length].toString()
                }
                .joinToString("")
        Box(modifier.clipToBounds()) {
            OreText(
                text,
                Modifier.graphicsLayer { alpha = 0f },
                color = color,
                style = style,
                maxLines = 1,
            )
            OreText(
                scrambled,
                Modifier.matchParentSize().clearAndSetSemantics {},
                color = color,
                style = style,
                maxLines = 1,
            )
        }
    }
}
