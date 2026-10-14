package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OrePixelArt
import kotlin.math.roundToInt

/** 窗口左侧页签竖条的宽度 */
const val SIDE_RAIL_WIDTH = 24

/** 页面四周的留白，横向两侧相同 */
const val PAGE_PADDING_X = 8
const val PAGE_PADDING_TOP = 7
const val PAGE_PADDING_BOTTOM = 8

private const val SWITCH_MILLIS = 150
private const val SWITCH_RISE_DP = 6

private val pagePadding =
    Modifier.padding(
        start = PAGE_PADDING_X.dp,
        end = PAGE_PADDING_X.dp,
        top = PAGE_PADDING_TOP.dp,
        bottom = PAGE_PADDING_BOTTOM.dp
    )

/**
 * BD 的标准窗口：顶部是 [header]，内容区左侧是页签竖条 [rail]，右侧是 [pages]。
 *
 * 通常有两页：[BdMainPage] 与设置用的 [BdTabPage]，竖条上方是主页面的页签，底部是设置的页签：
 * ```
 * var settingsOpen by remember { mutableStateOf(false) }
 * BdTabbedWindow(
 *     modifier,
 *     header = { BdHeader(icon, title, ::requestClose) },
 *     rail = {
 *         BdRailTab(title, BdGlyphs.Main, selected = !settingsOpen) { settingsOpen = false }
 *         Spacer(Modifier.weight(1f))
 *         BdRailTab(settings, OreGlyph.Gear.art, selected = settingsOpen) { settingsOpen = true }
 *     },
 * ) {
 *     BdMainPage(!settingsOpen) { ... }
 *     BdTabPage(settingsOpen) { ... }
 * }
 * ```
 * 再加一页就是再加一个页签与一个 [BdTabPage]。当前页由调用方保存；有槽位的界面离开主页面时还应停用槽位。
 * 竖条里也可以放页签以外的按钮。
 */
@Composable
fun BdTabbedWindow(
    modifier: Modifier,
    header: @Composable () -> Unit,
    rail: @Composable ColumnScope.() -> Unit,
    pages: @Composable BoxScope.() -> Unit,
) {
    val colors = Bd.colors
    BdWindow(Modifier.centerContent().then(modifier)) {
        header()
        Box {
            // 页面嵌在屏幕里，切换时新页面从下方浮起，裁剪到内容区
            Box(
                Modifier.padding(start = SIDE_RAIL_WIDTH.dp)
                    .bdScreen(
                        colors,
                        start = SCREEN_INSET_X.dp,
                        top = SCREEN_INSET_TOP.dp,
                        end = SCREEN_INSET_X.dp,
                        bottom = SCREEN_INSET_BOTTOM.dp,
                    )
                    .clipToBounds(),
                content = pages,
            )
            // 竖条在屏幕左侧的机壳上，高度跟随主页面；页签靠右，选中的页签接进屏幕
            Box(Modifier.matchParentSize()) {
                Column(
                    Modifier.width(SIDE_RAIL_WIDTH.dp).fillMaxHeight().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    content = rail,
                )
            }
        }
    }
}

/** 页面内容到屏幕边的距离，四周相同 */
private const val SCREEN_GAP = 5

/** 屏幕在页面区域四周让出的机壳宽度 */
const val SCREEN_INSET_X = PAGE_PADDING_X - SCREEN_GAP
const val SCREEN_INSET_TOP = PAGE_PADDING_TOP - SCREEN_GAP
const val SCREEN_INSET_BOTTOM = PAGE_PADDING_BOTTOM - SCREEN_GAP

/**
 * 让内容区而不是整个窗口居中：窗口左移半个竖条宽，屏幕两侧空位不够时少移一些。
 * 只在布局阶段移动，原生槽位的命中区域随之更新
 */
private fun Modifier.centerContent() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val room =
            if (constraints.hasBoundedWidth) ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0) else 0
        val shift = minOf(SIDE_RAIL_WIDTH.dp.roundToPx() / 2, room)
        layout(placeable.width, placeable.height) { placeable.place(-shift, 0) }
    }

/**
 * 竖条上的页签；[label] 是悬停提示。未选中的页签是机壳上凸起的按钮，
 * 选中的页签是屏幕伸出的一截：底色与发光边接进右侧的屏幕
 */
@Composable
fun BdRailTab(label: String, art: OrePixelArt, selected: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    BdTooltip(label, beside = true) {
        Box(
            Modifier.size(20.dp)
                .hoverable(interaction)
                .bdClickable(interaction, enabled = !selected, onClick = onClick)
                .then(
                    if (selected) Modifier.drawBehind {
                        val p = 1.dp.toPx()
                        // 伸到屏幕的发光边上，盖住它与页签相接的一段
                        val reach = size.width + (SCREEN_INSET_X + 1).dp.toPx()
                        drawRect(colors[BdColors.screen], Offset.Zero, Size(reach, size.height))
                        glowLine(colors, 0f, 0f, reach, horizontal = true, p)
                        glowLine(colors, 0f, size.height - p, reach, horizontal = true, p)
                        glowLine(colors, 0f, p, size.height - 2 * p, horizontal = false, p)
                    }
                    else Modifier.bdRaised(
                        colors,
                        fill = if (hovered) colors[BdColors.surface] else colors[BdColors.window],
                        edge = if (hovered) colors[BdColors.accent] else null,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            OreIcon(
                art,
                Modifier.size(9.dp),
                color =
                    when {
                        selected -> colors[BdColors.accentDeep]
                        hovered -> colors[BdColors.text]
                        else -> colors[BdColors.muted]
                    },
            )
        }
    }
}

/**
 * 页面的显示状态，由 [BdMainPage] 与 [BdTabPage] 提供给其中的内容。[current] 为当前打开的页，
 * [visible] 在切走后淡出结束前仍为 true。槽位只在当前页上响应点击
 */
@Immutable
data class BdPageVisibility(val current: Boolean, val visible: Boolean)

val LocalBdPageVisibility = compositionLocalOf { BdPageVisibility(current = true, visible = true) }

@Composable
private fun ProvidePageVisibility(shown: Boolean, progress: State<Float>, content: @Composable () -> Unit) {
    // 动画中只在可见与否变化时重组，不必每帧重组页面
    val visible by remember(shown) { derivedStateOf { shown || progress.value > 0f } }
    CompositionLocalProvider(LocalBdPageVisibility provides BdPageVisibility(shown, visible), content = content)
}

/** 主页面：决定内容区的大小，切到其他页时淡出 */
@Composable
fun BoxScope.BdMainPage(shown: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val progress = switchProgress(shown)
    ProvidePageVisibility(shown, progress) {
        Column(Modifier.graphicsLayer { alpha = progress.value }.then(pagePadding), content = content)
    }
}

/**
 * 其他页面：与主页面一样大，打开时盖住主页面并挡住它的点击，内容放不下时在页内滚动。
 * 隐藏期间不组合
 */
@Composable
fun BoxScope.BdTabPage(
    shown: Boolean,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val progress = switchProgress(shown)
    if (!shown && progress.value == 0f) return
    ProvidePageVisibility(shown, progress) {
        BdScrollColumn(
            Modifier.matchParentSize()
                .offset { IntOffset(0, ((1f - progress.value) * SWITCH_RISE_DP.dp.toPx()).roundToInt()) }
                .graphicsLayer { alpha = progress.value }
                // 没有控件的地方也接住指针，点击不会落到下层的主页面
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .then(pagePadding),
            verticalArrangement,
            content,
        )
    }
}

@Composable
private fun switchProgress(shown: Boolean) =
    animateFloatAsState(if (shown) 1f else 0f, tween(SWITCH_MILLIS, easing = FastOutSlowInEasing), label = "page")
