package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.ui.LocalUiFeedback
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OrePixelArt
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.overlay.OreTooltip
import dev.compixel.ui.ore.overlay.OreTooltipMode
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 窗口：深色半透明的玻璃面板加一像素细线框，透出背后的游戏画面；高度随内容 */
@Composable
fun BdWindow(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Bd.colors
    Column(modifier.background(colors.window).border(1.dp, colors.line), content = content)
}

/** 窗口标题栏：设备图标、模组名与标题，右侧是关闭按钮 */
@Composable
fun BdHeader(icon: ItemIcon?, title: String, onClose: () -> Unit, tag: String? = null) {
    val colors = Bd.colors
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(colors.surface).padding(start = 7.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            MinecraftItemIcon(icon, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            OreText("BEYOND DIMENSIONS", color = colors.faint, style = Bd.overline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom) {
                OreText(
                    title,
                    Modifier.weight(1f, fill = false),
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tag != null) {
                    Spacer(Modifier.width(3.dp))
                    OreText(tag, color = colors.faint, style = Bd.caption, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        BdGlyphButton(OreGlyph.Cross, null, onClose, size = 13.dp, glyphSize = 7.dp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
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
                .bdClickable(interaction, enabled = enabled, onClick = onClick)
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
    if (description != null) OreTooltip(description, mode = OreTooltipMode.Immediate) { button() } else button()
}

/** 分组标题：强调色方点、标题与延伸到边缘的细线 */
@Composable
fun BdSectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val colors = Bd.colors
    Row(modifier.fillMaxWidth().heightIn(min = 9.dp), verticalAlignment = Alignment.CenterVertically) {
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

/** 可点击的 BD 控件：与 Ore 控件一样，点击后发出原版的点击声 */
@Composable
fun Modifier.bdClickable(interaction: MutableInteractionSource, enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val feedback = LocalUiFeedback.current
    return clickable(interaction, indication = null, enabled = enabled) {
        onClick()
        feedback.activate()
    }
}

/** 数量标签：深色半透明底上的浅色小字 */
@Composable
fun BdAmountPill(text: String, modifier: Modifier = Modifier) {
    Box(modifier.offset(x = (-0.5).dp, y = (-0.5).dp).background(Bd.colors.pill).padding(start = 1.dp, top = 0.5.dp)) {
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
            .bdClickable(interaction, enabled = enabled, onClick = onClick)
            .background(if ((hovered || active) && enabled) colors.accentSoft else colors.surface, Bd.ChipShape)
            .border(1.dp, if ((hovered || active) && enabled) colors.accent else colors.line, Bd.ChipShape)
            .padding(horizontal = 5.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private data class BdSegmentBounds(val left: Dp, val width: Dp)

/** 互斥的模式：白色选中框随选项切换平滑移动，同时适应选项宽度 */
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
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val segments = remember(options) { mutableStateMapOf<Int, BdSegmentBounds>() }
    var trackWidth by remember { mutableStateOf(0.dp) }
    Box(
        modifier
            .height(15.dp)
            .clip(Bd.ChipShape)
            .background(colors.sunken, Bd.ChipShape)
            .border(1.dp, colors.line, Bd.ChipShape)
            .padding(1.dp)
            .alpha(if (enabled) 1f else 0.55f)
    ) {
        segments[selected]?.let { target ->
            val left by animateDpAsState(
                targetValue = target.left,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "BD tab position",
            )
            val width by animateDpAsState(
                targetValue = target.width,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "BD tab width",
            )
            val rightGap = trackWidth - left - width
            val startGap = if (layoutDirection == LayoutDirection.Ltr) left else rightGap
            val endGap = if (layoutDirection == LayoutDirection.Ltr) rightGap else left
            // 接近两端时才形成切角，滑动中也保持在外框以内。
            val segmentShape = CutCornerShape(
                topStart = (2.dp - startGap).coerceIn(0.dp, 2.dp),
                bottomEnd = (2.dp - endGap).coerceIn(0.dp, 2.dp),
            )
            // 选中框不参与轨道测量；轨道尺寸只由实际选项决定。
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier.absoluteOffset(x = left)
                        .width(width)
                        .fillMaxHeight()
                        .background(colors.surface, segmentShape)
                        .border(1.dp, colors.accent, segmentShape)
                )
            }
        }
        Row(
            Modifier.fillMaxHeight().onSizeChanged { size ->
                trackWidth = with(density) { size.width.toDp() }
            }
        ) {
            options.forEachIndexed { index, option ->
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val chosen = index == selected
                Box(
                    (if (fill) Modifier.weight(1f) else Modifier)
                        .fillMaxHeight()
                        .hoverable(interaction, enabled)
                        .bdClickable(interaction, enabled = enabled && !chosen) { onSelect(index) }
                        .onGloballyPositioned { position ->
                            segments[index] = with(density) {
                                BdSegmentBounds(position.positionInParent().x.toDp(), position.size.width.toDp())
                            }
                        }
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
}

/**
 * 开关：细线框的深色轨道里一个小方块。关闭时方块在左侧、是灰色；开启时轨道染上一层强调色、边框变为强调色，
 * 方块移到右侧并亮起。位置与颜色一起过渡。
 */
@Composable
fun BdToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val pointerOver by interaction.collectIsHoveredAsState()
    val hovered = pointerOver && enabled
    val motion = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
    val progress by animateFloatAsState(if (checked) 1f else 0f, motion, label = "BD switch")
    val edge = if (hovered) colors.lineStrong else colors.line
    val edgeOn = colors.accent.copy(alpha = if (hovered) 1f else 0.6f)
    Box(
        Modifier.size(20.dp, 10.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .hoverable(interaction, enabled)
            .bdClickable(interaction, enabled = enabled) { onCheckedChange(!checked) }
            .background(lerp(colors.sunken, colors.accentSoft, progress))
            .border(1.dp, lerp(edge, edgeOn, progress))
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = 10.dp * progress).size(6.dp).background(lerp(colors.muted, colors.accent, progress)))
    }
}

/** 一行设置：左侧标题与说明，右侧控件 */
@Composable
fun BdSettingRow(title: String, description: String? = null, control: @Composable () -> Unit) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            OreText(title, color = colors.text, maxLines = 1)
            if (description != null) OreText(description, color = colors.faint, style = Bd.caption, maxLines = 2)
        }
        Spacer(Modifier.width(4.dp))
        control()
    }
}

/**
 * 一组互斥模式：标题在上，下面是占满宽度的分段按钮；选项多或文字长时用 [cycle] 改为左右切换。
 * [description] 是当前选项的说明，显示在最下方。
 */
@Composable
fun BdModeSetting(
    title: String,
    options: List<String>,
    selected: Int,
    enabled: Boolean,
    description: String? = null,
    cycle: Boolean = false,
    onSelect: (Int) -> Unit,
) {
    val colors = Bd.colors
    Column(Modifier.fillMaxWidth()) {
        OreText(title, color = colors.text, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        if (cycle) BdCycler(options, selected, enabled, onSelect)
        else BdSegmented(options, selected, onSelect, Modifier.fillMaxWidth(), enabled = enabled, fill = true)
        if (description != null) {
            Spacer(Modifier.height(2.dp))
            OreText(description, color = colors.faint, style = Bd.caption, maxLines = 2)
        }
    }
}

/** 左右箭头依次切换的选择器 */
@Composable
private fun BdCycler(options: List<String>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val colors = Bd.colors
    Row(
        Modifier.fillMaxWidth()
            .height(15.dp)
            .background(colors.sunken, Bd.ChipShape)
            .border(1.dp, colors.line, Bd.ChipShape)
            .padding(1.dp)
            .clip(Bd.ChipInnerShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BdGlyphButton(OreGlyph.ChevronLeft, null, { onSelect((selected - 1).mod(options.size)) }, enabled = enabled, size = 13.dp, glyphSize = 6.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            OreText(options.getOrElse(selected) { "" }, color = if (enabled) colors.accentDeep else colors.faint, maxLines = 1)
        }
        BdGlyphButton(OreGlyph.ChevronRight, null, { onSelect((selected + 1).mod(options.size)) }, enabled = enabled, size = 13.dp, glyphSize = 6.dp)
    }
}

/** 整数输入框：只接受数字，超出范围时收回到边界；每次输入都提交 */
@Composable
fun BdNumberField(value: Int, range: IntRange, enabled: Boolean, onCommit: (Int) -> Unit) {
    val colors = Bd.colors
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if (text.toIntOrNull() != value) text = value.toString() }
    BasicTextField(
        text,
        { input ->
            val digits = input.filter(Char::isDigit).take(range.last.toString().length)
            text = digits
            digits.toIntOrNull()?.let { onCommit(it.coerceIn(range)) }
        },
        Modifier.width(44.dp).height(15.dp),
        enabled = enabled,
        singleLine = true,
        textStyle = Bd.body.copy(color = colors.text, textAlign = TextAlign.End),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxSize().background(colors.surface, Bd.ChipShape).border(1.dp, colors.line, Bd.ChipShape).padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                inner()
            }
        },
    )
}

/** 纵向内容：放不下时可滚动，右侧出现与存储格子相同的细滚动条 */
@Composable
fun BdScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    // 第一次布局前 maxValue 是表示未知的 Int.MAX_VALUE，这时按放得下处理，否则刚出现的一帧会带上滚动条与右侧留白
    val scrollable = scroll.maxValue in 1 until Int.MAX_VALUE
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(end = if (scrollable) (SCROLLBAR_WIDTH + 2).dp else 0.dp).verticalScroll(scroll),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        // 滚动条只跟随内容的高度，不参与测量，不会把容器撑到最大高度
        if (scrollable) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) {
                BdScrollbar(scroll, Modifier.width(SCROLLBAR_WIDTH.dp).fillMaxHeight())
            }
        }
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

/** 细滚动条的宽度；内容与它之间另留 2 */
const val SCROLLBAR_WIDTH = 5

/** 按行滚动的细滚动条，例如存储格子。[first] 是第一个可见行，[visible] 与 [total] 为可见行数与总行数 */
@Composable
fun BdScrollbar(first: Int, visible: Int, total: Int, onScrollTo: (Int) -> Unit, modifier: Modifier = Modifier) {
    val maxFirst = (total - visible).coerceAtLeast(0)
    val latest by rememberUpdatedState(onScrollTo)
    BdScrollbarTrack(
        visibleFraction = if (total > 0) visible.toFloat() / total else 1f,
        position = if (maxFirst > 0) first.toFloat() / maxFirst else 0f,
        canScroll = maxFirst > 0,
        onScrollTo = { latest((it * maxFirst).roundToInt()) },
        modifier = modifier,
    )
}

/** 跟随 [state] 的细滚动条，与按行滚动的外观相同 */
@Composable
fun BdScrollbar(state: ScrollState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    // 第一次布局前 maxValue 是表示未知的 Int.MAX_VALUE
    val maximum = if (state.maxValue == Int.MAX_VALUE) 0 else state.maxValue
    val content = maximum + state.viewportSize
    BdScrollbarTrack(
        visibleFraction = if (content > 0) state.viewportSize.toFloat() / content else 1f,
        position = if (maximum > 0) state.value.toFloat() / maximum else 0f,
        canScroll = maximum > 0,
        onScrollTo = { scope.launch { state.scrollTo((it * maximum).roundToInt()) } },
        modifier = modifier,
    )
}

/**
 * 两种滚动条共用的外观与操作：一像素细线框的深色凹槽里，一条标志渐变的平直滑块，长度对应可见的比例，至少 12。
 * 点击或拖动轨道时，滑块中心跳到指针处；[onScrollTo] 收到 0 到 1 之间的位置。
 */
@Composable
private fun BdScrollbarTrack(
    visibleFraction: Float,
    position: Float,
    canScroll: Boolean,
    onScrollTo: (Float) -> Unit,
    modifier: Modifier,
) {
    val colors = Bd.colors
    val latest by rememberUpdatedState(onScrollTo)
    val density = LocalDensity.current
    BoxWithConstraints(modifier.background(colors.sunken).border(1.dp, colors.line).padding(1.dp)) {
        val track = maxHeight
        val thumb = (track * visibleFraction.coerceIn(0f, 1f)).coerceIn(minOf(12.dp, track), track)
        val travel = track - thumb
        fun positionAt(y: Float): Float =
            with(density) { ((y - thumb.toPx() / 2f) / travel.toPx().coerceAtLeast(1f)).coerceIn(0f, 1f) }
        Box(
            Modifier.fillMaxSize()
                .pointerInput(travel, thumb) { detectTapGestures { latest(positionAt(it.y)) } }
                .pointerInput(travel, thumb) {
                    detectVerticalDragGestures { change, _ -> latest(positionAt(change.position.y)) }
                }
        ) {
            if (canScroll) {
                Box(
                    Modifier.offset(y = travel * position.coerceIn(0f, 1f))
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
        Modifier.height(15.dp)
            .background(colors.sunken, Bd.ChipShape)
            .border(1.dp, colors.line, Bd.ChipShape)
            .padding(1.dp)
            .clip(Bd.ChipInnerShape),
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
            Modifier.shadow(6.dp, Bd.ChipShape, ambientColor = Color(0x33000000), spotColor = Color(0x66000000))
                .background(colors.popover, Bd.ChipShape)
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
