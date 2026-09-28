package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import dev.composemc.forge.item.ItemIcon
import dev.composemc.forge.item.MinecraftItemIcon
import dev.composemc.ui.ore.display.OreGlyph
import dev.composemc.ui.ore.display.OreIcon
import dev.composemc.ui.ore.display.OrePixelArt
import dev.composemc.ui.ore.display.OreText
import dev.composemc.ui.ore.overlay.OreTooltip
import kotlin.math.roundToInt

/** 窗口：切角的冰白面板，细线边框与柔和阴影；高度随内容 */
@Composable
fun BdWindow(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Bd.colors
    Column(
        modifier
            .shadow(10.dp, Bd.WindowShape, ambientColor = Color(0x330B1420), spotColor = Color(0x660B1420))
            .clip(Bd.WindowShape)
            .background(colors.window)
            .border(1.dp, colors.lineStrong, Bd.WindowShape),
        content = content,
    )
}

enum class BdTone {
    Online,
    Warning,
    Offline,
}

class BdStatus(val text: String, val tone: BdTone)

/** 窗口标题栏：顶端的标志渐变线、设备图标、所属网络与状态 */
@Composable
fun BdHeader(
    icon: ItemIcon?,
    overline: String,
    title: String,
    tag: String? = null,
    status: BdStatus? = null,
    onClose: (() -> Unit)? = null,
) {
    val colors = Bd.colors
    Box(Modifier.fillMaxWidth().height(2.dp).background(colors.signature))
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(colors.surface).padding(start = 7.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            MinecraftItemIcon(icon, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            OreText(overline, color = colors.faint, style = Bd.overline, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom) {
                OreText(title, Modifier.weight(1f, fill = false), color = colors.text, maxLines = 1)
                if (tag != null) {
                    Spacer(Modifier.width(3.dp))
                    OreText(tag, color = colors.faint, style = Bd.caption, maxLines = 1)
                }
            }
        }
        if (status != null) {
            Spacer(Modifier.width(4.dp))
            BdStatusChip(status)
        }
        if (onClose != null) {
            Spacer(Modifier.width(4.dp))
            BdCloseButton(onClose)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

@Composable
fun BdStatusChip(status: BdStatus) {
    val colors = Bd.colors
    val (ink, fill, line) =
        when (status.tone) {
            BdTone.Online -> Triple(colors.online, colors.onlineSoft, colors.onlineLine)
            BdTone.Warning -> Triple(colors.warning, colors.warningSoft, colors.warningLine)
            BdTone.Offline -> Triple(colors.muted, colors.sunken, colors.line)
        }
    Row(
        Modifier.border(1.dp, line, Bd.ChipShape)
            .background(fill, Bd.ChipShape)
            .padding(start = 3.dp, end = 4.dp, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(3.dp).background(ink))
        Spacer(Modifier.width(3.dp))
        OreText(status.text, color = ink, style = Bd.caption, maxLines = 1)
    }
}

@Composable
fun BdCloseButton(onClick: () -> Unit) {
    BdGlyphButton(OreGlyph.Cross, null, onClick, size = 13.dp, glyphSize = 7.dp)
}

/** 只有图标的小按钮 */
@Composable
fun BdGlyphButton(
    glyph: OreGlyph,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = 16.dp,
    glyphSize: Dp = 8.dp,
) = BdGlyphButton(glyph.art, description, onClick, modifier, enabled, selected, size, glyphSize)

@Composable
fun BdGlyphButton(
    art: OrePixelArt,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = 16.dp,
    glyphSize: Dp = 8.dp,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val button: @Composable () -> Unit = {
        Box(
            modifier
                .size(size)
                .hoverable(interaction, enabled)
                .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
                .background(
                    when {
                        selected -> colors.accentSoft
                        hovered && enabled -> colors.accentSoft
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            val tint =
                when {
                    !enabled -> colors.faint
                    selected || hovered -> colors.accentDeep
                    else -> colors.muted
                }
            OreIcon(art, Modifier.size(glyphSize), color = tint)
        }
    }
    if (description != null) OreTooltip(description) { button() } else button()
}

/** 分组标题：强调色方点、标题与延伸到边缘的细线 */
@Composable
fun BdSectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val colors = Bd.colors
    Row(modifier.fillMaxWidth().height(9.dp), verticalAlignment = Alignment.CenterVertically) {
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

/** 四个 L 形角标：格子悬停时的“锁定”效果 */
fun Modifier.brackets(color: Color, arm: Dp = 4.dp, stroke: Dp = 1.dp): Modifier = drawWithContent {
    drawContent()
    val a = arm.toPx()
    val s = stroke.toPx()
    val w = size.width
    val h = size.height
    drawRect(color, Offset(0f, 0f), Size(a, s))
    drawRect(color, Offset(0f, 0f), Size(s, a))
    drawRect(color, Offset(w - a, 0f), Size(a, s))
    drawRect(color, Offset(w - s, 0f), Size(s, a))
    drawRect(color, Offset(0f, h - s), Size(a, s))
    drawRect(color, Offset(0f, h - a), Size(s, a))
    drawRect(color, Offset(w - a, h - s), Size(a, s))
    drawRect(color, Offset(w - s, h - a), Size(s, a))
}

/** 数量标签：白色半透明底上的深色小字 */
@Composable
fun BdAmountPill(text: String, modifier: Modifier = Modifier) {
    Box(modifier.offset(x = (-0.5).dp, y = (-0.5).dp).background(Color(0xD9FFFFFF)).padding(start = 1.dp, top = 0.5.dp)) {
        OreText(text, color = Bd.colors.text, style = Bd.amount, maxLines = 1)
    }
}

/** 单行搜索框；右键清空 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BdSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    BasicTextField(
        value,
        onValueChange,
        modifier.height(18.dp).hoverable(interaction).onPointerEvent(PointerEventType.Press) {
            if (it.buttons.isSecondaryPressed) onValueChange("")
        },
        singleLine = true,
        textStyle = Bd.body.copy(color = colors.text),
        interactionSource = interaction,
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth()
                    .fillMaxHeight()
                    .background(colors.surface, Bd.ChipShape)
                    .border(
                        1.dp,
                        when {
                            focused -> colors.accent
                            hovered -> colors.lineStrong
                            else -> colors.line
                        },
                        Bd.ChipShape,
                    )
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OreIcon(OreGlyph.MagnifyingGlass, Modifier.size(8.dp), color = if (focused) colors.accent else colors.faint)
                Spacer(Modifier.width(4.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) OreText(placeholder, color = colors.faint, maxLines = 1)
                    inner()
                }
                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    BdGlyphButton(OreGlyph.Cross, null, { onValueChange("") }, size = 10.dp, glyphSize = 6.dp)
                }
            }
        },
    )
}

/** 带边框的小按钮，用于视图选项等 */
@Composable
fun BdChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier
            .height(18.dp)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .background(if ((hovered || active) && enabled) colors.accentSoft else colors.surface, Bd.ChipShape)
            .border(1.dp, if ((hovered || active) && enabled) colors.accent else colors.line, Bd.ChipShape)
            .padding(horizontal = 5.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 互斥的模式：选中项是带天蓝边框的白色按键 */
@Composable
fun BdSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Boolean = false,
) {
    val colors = Bd.colors
    Row(
        modifier
            .height(15.dp)
            .background(colors.sunken, Bd.ChipShape)
            .border(1.dp, colors.line, Bd.ChipShape)
            .padding(1.dp)
            .alpha(if (enabled) 1f else 0.55f)
    ) {
        options.forEachIndexed { index, option ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val chosen = index == selected
            Box(
                (if (fill) Modifier.weight(1f) else Modifier)
                    .fillMaxHeight()
                    .hoverable(interaction, enabled)
                    .clickable(interaction, indication = null, enabled = enabled && !chosen) { onSelect(index) }
                    .then(if (chosen) Modifier.background(colors.surface).border(1.dp, colors.accent) else Modifier)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                OreText(
                    option,
                    color =
                        when {
                            chosen -> colors.accentDeep
                            hovered && enabled -> colors.text
                            else -> colors.muted
                        },
                    maxLines = 1,
                )
            }
        }
    }
}

/** 开关：开启时轨道为标志渐变，关闭时为凹陷底色，方形白色滑块 */
@Composable
fun BdToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier.size(22.dp, 11.dp)
            .hoverable(interaction, enabled)
            .clickable(interaction, indication = null, enabled = enabled) { onCheckedChange(!checked) }
            .background(if (checked) colors.signature else SolidColor(colors.sunken), Bd.ChipShape)
            .border(
                1.dp,
                when {
                    checked -> colors.accentDeep.copy(alpha = 0.35f)
                    hovered && enabled -> colors.lineStrong
                    else -> colors.line
                },
                Bd.ChipShape,
            )
            .padding(2.dp)
            .alpha(if (enabled) 1f else 0.55f),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(7.dp).background(colors.surface).border(1.dp, if (checked) Color.Transparent else colors.lineStrong))
    }
}

/** 一行设置：左侧标题与说明，右侧控件 */
@Composable
fun BdSettingRow(title: String, description: String? = null, control: @Composable () -> Unit) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            OreText(title, color = colors.text, maxLines = 1)
            if (description != null) OreText(description, color = colors.faint, style = Bd.caption, maxLines = 1)
        }
        Spacer(Modifier.width(4.dp))
        control()
    }
}

/** 一组互斥模式：标题在上，分段按钮占满宽度 */
@Composable
fun BdModeSetting(title: String, options: List<String>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        OreText(title, color = Bd.colors.text, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        BdSegmented(options, selected, onSelect, Modifier.fillMaxWidth(), enabled = enabled, fill = true)
    }
}

/**
 * 分段仪表：等宽小格、一像素间隔，沿标志渐变填充。
 * [cell] 为每格含间隔的宽度。
 */
@Composable
fun BdMeter(fraction: Float, modifier: Modifier, cell: Dp = 3.dp) {
    val colors = Bd.colors
    Box(
        modifier.background(colors.surface).border(1.dp, colors.line).padding(2.dp).drawBehind {
            val pitch = cell.toPx()
            val gap = (1.dp.toPx() / 2f).coerceAtLeast(1f)
            val count = (size.width / pitch).toInt().coerceAtLeast(1)
            val filled = (count * fraction.coerceIn(0f, 1f)).roundToInt()
            val start = (size.width - count * pitch + gap) / 2f
            for (i in 0 until count) {
                val color = if (i < filled) lerp(colors.cyan, colors.violet, i / (count - 1f).coerceAtLeast(1f)) else colors.sunken
                drawRect(color, Offset(start + i * pitch, 0f), Size(pitch - gap, size.height))
            }
        }
    )
}

/**
 * 按行滚动的细滚动条。[first] 是第一行可见行，[visible] 与 [total] 为可见行数与总行数。
 */
@Composable
fun BdScrollbar(first: Int, visible: Int, total: Int, onScrollTo: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Bd.colors
    val maxFirst = (total - visible).coerceAtLeast(0)
    val latestScroll by rememberUpdatedState(onScrollTo)
    val density = LocalDensity.current
    BoxWithConstraints(modifier.background(colors.sunken).border(1.dp, colors.line)) {
        val track = maxHeight - 2.dp
        val thumb = if (total <= 0) track else (track * (visible.toFloat() / total)).coerceIn(12.dp, track)
        val travel = track - thumb
        val offset = if (maxFirst == 0) 0.dp else travel * (first.toFloat() / maxFirst)
        fun rowAt(y: Float): Int {
            if (maxFirst == 0) return 0
            val travelPx = with(density) { travel.toPx() }
            val thumbPx = with(density) { thumb.toPx() }
            val fraction = ((y - thumbPx / 2f) / travelPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
            return (fraction * maxFirst).roundToInt()
        }
        Box(
            Modifier.fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(maxFirst, travel, thumb) {
                    detectTapGestures { latestScroll(rowAt(it.y)) }
                }
                .pointerInput(maxFirst, travel, thumb) {
                    detectVerticalDragGestures { change, _ -> latestScroll(rowAt(change.position.y)) }
                }
        ) {
            if (maxFirst > 0) {
                Box(
                    Modifier.offset(y = offset + 1.dp)
                        .padding(horizontal = 1.dp)
                        .fillMaxWidth()
                        .height(thumb)
                        .background(colors.signatureVertical)
                )
            }
        }
    }
}

/** 数值加减器 */
@Composable
fun BdStepper(value: Int, onChange: (Int) -> Unit, range: IntRange, enabled: Boolean = true) {
    val colors = Bd.colors
    Row(
        Modifier.height(15.dp).background(colors.sunken, Bd.ChipShape).border(1.dp, colors.line, Bd.ChipShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BdGlyphButton(OreGlyph.Minus, null, { onChange(value - 1) }, enabled = enabled && value > range.first, size = 13.dp, glyphSize = 6.dp)
        Box(Modifier.width(IntrinsicSize.Min).padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
            OreText(value.toString(), color = colors.text, maxLines = 1)
        }
        BdGlyphButton(OreGlyph.Plus, null, { onChange(value + 1) }, enabled = enabled && value < range.last, size = 13.dp, glyphSize = 6.dp)
    }
}

/** 贴在锚点右侧（放不下时在左侧）的浮层卡片；点击外部关闭 */
@Composable
fun BdPopover(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = Bd.colors
    val gap = with(LocalDensity.current) { 3.dp.roundToPx() }
    val position = remember(gap) { BesideAnchor(gap) }
    Popup(position, onDismissRequest, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier.shadow(6.dp, Bd.ChipShape, ambientColor = Color(0x330B1420), spotColor = Color(0x550B1420))
                .background(colors.surface, Bd.ChipShape)
                .border(1.dp, colors.lineStrong, Bd.ChipShape)
                .padding(6.dp),
            content = content,
        )
    }
}

private class BesideAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.right + gap
        if (x + popupContentSize.width > windowSize.width) x = anchorBounds.left - gap - popupContentSize.width
        val y = anchorBounds.top.coerceAtMost(windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x.coerceAtLeast(0), y)
    }
}
