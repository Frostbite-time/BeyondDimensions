package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import dev.compixel.ui.ore.display.OreText
import kotlin.math.roundToInt

/*
 * 浮在界面上的几种层：悬停提示、点开的浮层与文本框的右键菜单。
 * 三者同一种外观：比窗口更实的深色底、一像素浅色细线框与控件的切角，提示框的上沿是一条标志渐变线。
 * 界面退场时窗口失去焦点，它们随即消失，不会停在淡出的画面上。
 */

/** 悬停提示：指针停在 [content] 上时显示 [text]；[beside] 为 true 时提示在右侧，否则在下方 */
@Composable
fun BdTooltip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    beside: Boolean = false,
    content: @Composable () -> Unit,
) =
    BdTooltip(
        { OreText(text, color = Bd.colors.text, style = Bd.caption) },
        modifier,
        enabled && text.isNotBlank(),
        beside,
        content,
    )

/** 内容自定的悬停提示，例如多行的说明 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BdTooltip(
    tooltip: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    beside: Boolean = false,
    content: @Composable () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    Box(
        modifier
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
    ) {
        content()
        if (hovered && enabled && LocalWindowInfo.current.isWindowFocused) {
            val gap = with(LocalDensity.current) { 3.dp.roundToPx() }
            Popup(remember(gap, beside) { TooltipPosition(gap, beside) }) {
                val colors = Bd.colors
                Column(
                    Modifier.widthIn(max = 200.dp)
                        .background(colors.popover, Bd.ChipShape)
                        .border(1.dp, colors.lineStrong, Bd.ChipShape)
                        .drawWithContent {
                            drawContent()
                            // 上沿从切角之后开始换成标志渐变
                            val cut = Bd.CHIP_CUT.dp.toPx()
                            drawRect(colors.signature, Offset(cut, 0f), Size(size.width - cut, 1.dp.toPx()))
                        }
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    content = tooltip,
                )
            }
        }
    }
}

/** 提示框的位置：锚点下方居中，下方放不下时在上方；或锚点右侧居中，右侧放不下时在左侧。都不超出窗口 */
private class TooltipPosition(private val gap: Int, private val beside: Boolean) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        val x: Int
        val y: Int
        if (beside) {
            x = (anchorBounds.right + gap).takeIf { it + width <= windowSize.width } ?: (anchorBounds.left - gap - width)
            y = anchorBounds.center.y - height / 2
        } else {
            x = anchorBounds.center.x - width / 2
            y = (anchorBounds.bottom + gap).takeIf { it + height <= windowSize.height } ?: (anchorBounds.top - gap - height)
        }
        return IntOffset(x.coerceIn(0, (windowSize.width - width).coerceAtLeast(0)), y.coerceIn(0, (windowSize.height - height).coerceAtLeast(0)))
    }
}

/** 点开的浮层，例如下拉的选项：在锚点下方、与锚点右边对齐，下方放不下时在上方；点击外部关闭 */
@Composable
fun BdPopover(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!LocalWindowInfo.current.isWindowFocused) return
    val gap = with(LocalDensity.current) { 2.dp.roundToPx() }
    Popup(remember(gap) { BelowAnchor(gap) }, onDismissRequest, properties = PopupProperties(focusable = true)) {
        // 按内容最宽的一行定宽：浮层的约束是整个窗口，占满宽度的行不能把浮层撑到全屏
        OverlayCard(Modifier.width(IntrinsicSize.Max).padding(6.dp), content)
    }
}

private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        val x = anchorBounds.right - width
        val y = (anchorBounds.bottom + gap).takeIf { it + height <= windowSize.height } ?: (anchorBounds.top - gap - height)
        return IntOffset(x.coerceIn(0, (windowSize.width - width).coerceAtLeast(0)), y.coerceIn(0, (windowSize.height - height).coerceAtLeast(0)))
    }
}

@Composable
private fun OverlayCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Bd.colors
    Column(
        Modifier.background(colors.popover, Bd.ChipShape).border(1.dp, colors.lineStrong, Bd.ChipShape).then(modifier),
        content = content,
    )
}

/**
 * BD 文本框的右键菜单：剪切、复制、粘贴等由 Compose 按选区给出，这里只负责外观。
 * 在指针处展开，每项一行小字，与浮层中的选项同高。
 */
object BdContextMenu : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status
        if (status !is ContextMenuState.Status.Open || !LocalWindowInfo.current.isWindowFocused) return
        val close = { state.status = ContextMenuState.Status.Closed }
        val point = IntOffset(status.rect.center.x.roundToInt(), status.rect.center.y.roundToInt())
        Popup(remember(point) { AtPoint(point) }, close, properties = PopupProperties(focusable = true)) {
            OverlayCard(Modifier.width(IntrinsicSize.Max).widthIn(min = 40.dp).padding(1.dp)) {
                for (item in items()) {
                    MenuRow(item.label) {
                        close()
                        item.onClick()
                    }
                }
            }
        }
    }
}

/** 左上角落在锚点内 [point] 处，放不下时翻到指针的左侧或上方 */
private class AtPoint(private val point: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        val px = anchorBounds.left + point.x
        val py = anchorBounds.top + point.y
        val x = px.takeIf { it + width <= windowSize.width } ?: (px - width)
        val y = py.takeIf { it + height <= windowSize.height } ?: (py - height)
        return IntOffset(x.coerceIn(0, (windowSize.width - width).coerceAtLeast(0)), y.coerceIn(0, (windowSize.height - height).coerceAtLeast(0)))
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier.fillMaxWidth()
            .height(13.dp)
            .hoverable(interaction)
            .bdClickable(interaction, onClick = onClick)
            .background(if (hovered) colors.accentSoft else Color.Transparent)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        OreText(label, color = if (hovered) colors.accentDeep else colors.text, style = Bd.caption, maxLines = 1)
    }
}
