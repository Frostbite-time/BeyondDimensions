package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OrePixelArt
import dev.compixel.ui.ore.overlay.OreTooltip
import dev.compixel.ui.ore.overlay.OreTooltipMode
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
    Modifier.padding(start = PAGE_PADDING_X.dp, end = PAGE_PADDING_X.dp, top = PAGE_PADDING_TOP.dp, bottom = PAGE_PADDING_BOTTOM.dp)

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
            Box(Modifier.padding(start = SIDE_RAIL_WIDTH.dp), content = pages)
            // 竖条高度跟随主页面：深一层的底色，右侧一条细线
            Box(Modifier.matchParentSize()) {
                Column(
                    Modifier.width(SIDE_RAIL_WIDTH.dp)
                        .fillMaxHeight()
                        .background(colors.sunken)
                        .drawBehind { drawLine(colors.line, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1.dp.toPx()) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    content = rail,
                )
            }
        }
    }
}

/**
 * 让内容区（竖条右侧）而不是整个窗口落在居中的位置：窗口左移半个竖条宽；屏幕两侧的空位不够时少移一些，窗口不会移出屏幕。
 * 只在布局阶段移动摆放位置，原生槽位的命中区域随之更新。
 */
private fun Modifier.centerContent() =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val room = if (constraints.hasBoundedWidth) ((constraints.maxWidth - placeable.width) / 2).coerceAtLeast(0) else 0
        val shift = minOf(SIDE_RAIL_WIDTH.dp.roundToPx() / 2, room)
        layout(placeable.width, placeable.height) { placeable.place(-shift, 0) }
    }

/** 竖条上的页签：选中时有底色、细框与左侧的标志渐变线；[label] 是悬停提示 */
@Composable
fun BdRailTab(label: String, art: OrePixelArt, selected: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    OreTooltip(label, mode = OreTooltipMode.Immediate) {
        Box(
            Modifier.size(20.dp)
                .hoverable(interaction)
                .bdClickable(interaction, enabled = !selected, onClick = onClick)
                .then(
                    when {
                        selected -> Modifier.background(colors.surface).border(1.dp, colors.line)
                        hovered -> Modifier.background(colors.accentSoft)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.align(Alignment.CenterStart).width(2.dp).fillMaxHeight().background(colors.signatureVertical))
            OreIcon(
                art,
                Modifier.size(9.dp),
                color =
                    when {
                        selected -> colors.accentDeep
                        hovered -> colors.text
                        else -> colors.muted
                    },
            )
        }
    }
}

/** 主页面：决定内容区的大小；切到其他页时淡出，免得与盖在上面的半透明页面叠在一起 */
@Composable
fun BoxScope.BdMainPage(shown: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val progress by switchProgress(shown)
    Column(Modifier.graphicsLayer { alpha = progress }.then(pagePadding), content = content)
}

/**
 * 其他页面：与主页面一样大，切到这一页时从下方浮起并淡入，盖住主页面并挡住它的点击；内容放不下时在页内滚动。
 * 隐藏期间不组合，再次打开时滚动位置回到顶部。
 */
@Composable
fun BoxScope.BdTabPage(
    shown: Boolean,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val progress by switchProgress(shown)
    if (!shown && progress == 0f) return
    BdScrollColumn(
        Modifier.matchParentSize()
            .offset { IntOffset(0, ((1f - progress) * SWITCH_RISE_DP.dp.toPx()).roundToInt()) }
            .graphicsLayer { alpha = progress }
            // 没有控件的地方也接住指针，点击不会落到下层的主页面
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .then(pagePadding),
        verticalArrangement,
        content,
    )
}

@Composable
private fun switchProgress(shown: Boolean) =
    animateFloatAsState(if (shown) 1f else 0f, tween(SWITCH_MILLIS, easing = FastOutSlowInEasing), label = "page")
