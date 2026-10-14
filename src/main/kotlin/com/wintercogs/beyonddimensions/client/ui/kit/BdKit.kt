package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.ui.LocalUiFeedback
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.theme.LocalColorEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 窗口：一块机壳，深色外轮廓与一像素的明暗边，高度随内容 */
@Composable
fun BdWindow(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.bdRaised(Bd.colors), content = content)
}

/** 窗口标题栏：机壳上的图标与标题，右侧是调色板与关闭按钮 */
@Composable
fun BdHeader(icon: ItemIcon?, title: String, onClose: () -> Unit, tag: String? = null) {
    val openColorEditor = LocalColorEditor.current
    val colors = Bd.colors
    Row(
        Modifier.fillMaxWidth().height(25.dp).padding(start = 7.dp, end = 6.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            MinecraftItemIcon(icon, Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            OreText(
                title,
                Modifier.weight(1f, fill = false),
                color = colors[BdColors.text],
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tag != null) {
                Spacer(Modifier.width(4.dp))
                OreText(tag, color = colors[BdColors.faint], maxLines = 1)
            }
        }
        Spacer(Modifier.width(4.dp))
        if (openColorEditor != null) {
            BdGlyphButton(BdIcons.Palette, null, openColorEditor, size = 13.dp)
            Spacer(Modifier.width(2.dp))
        }
        BdGlyphButton(BdIcons.Close, null, onClose, size = 13.dp)
    }
}

/** 只有图标的小按钮 */
@Composable
fun BdGlyphButton(
    icon: BdPixelIcon,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = 16.dp,
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
                        selected -> colors[BdColors.accentSoft]
                        hovered && enabled -> colors[BdColors.accentSoft]
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            val tint =
                when {
                    !enabled -> colors[BdColors.faint]
                    selected || hovered -> colors[BdColors.accentDeep]
                    else -> colors[BdColors.muted]
                }
            BdIcon(icon, color = tint)
        }
    }
    if (description != null) BdTooltip(description) { button() } else button()
}

/** 分组标题：一行次要文字，像原版容器里的“物品栏”；[trailing] 放在行末 */
@Composable
fun BdSectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val colors = Bd.colors
    Row(modifier.fillMaxWidth().heightIn(min = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        OreText(text, Modifier.weight(1f), color = colors[BdColors.muted], style = Bd.overline, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

/** 四个 L 形角标：槽位悬停或选中时的“锁定”效果。画在边缘向内一个像素处，不压住槽位的明暗边 */
fun Modifier.brackets(color: Color, arm: Dp = 4.dp, stroke: Dp = 1.dp): Modifier = drawWithContent {
    drawContent()
    val a = arm.toPx()
    val s = stroke.toPx()
    val left = 1.dp.toPx()
    val top = left
    val right = size.width - left
    val bottom = size.height - left
    drawRect(color, Offset(left, top), Size(a, s))
    drawRect(color, Offset(left, top), Size(s, a))
    drawRect(color, Offset(right - a, top), Size(a, s))
    drawRect(color, Offset(right - s, top), Size(s, a))
    drawRect(color, Offset(left, bottom - s), Size(a, s))
    drawRect(color, Offset(left, bottom - a), Size(s, a))
    drawRect(color, Offset(right - a, bottom - s), Size(a, s))
    drawRect(color, Offset(right - s, bottom - a), Size(s, a))
}

/** 可点击的 BD 控件，点击后发出原版的点击声 */
@Composable
fun Modifier.bdClickable(
    interaction: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val feedback = LocalUiFeedback.current
    return clickable(interaction, indication = null, enabled = enabled) {
        onClick()
        feedback.activate()
    }
}

/** 数量标签：像原版的物品数量那样带一层错开的投影，颜色取自配色的 amount 与 amountShadow */
@Composable
fun BdAmountPill(text: String, modifier: Modifier = Modifier) {
    val colors = Bd.colors
    val shadow = with(LocalDensity.current) { AMOUNT_FONT_PIXEL.toPx() }
    // 四个字符正好占满槽位的宽度；放不下时向左伸出，不截掉末尾的单位
    Box(
        modifier.wrapContentWidth(Alignment.End, unbounded = true)
            .padding(end = AMOUNT_FONT_PIXEL, bottom = AMOUNT_FONT_PIXEL)
    ) {
        BasicText(
            remember(text) { narrowDots(text) },
            // 投影是一份错开的实心字，不模糊
            style = Bd.amount.copy(
                color = colors[BdColors.amount],
                shadow = Shadow(colors[BdColors.amountShadow], Offset(shadow, shadow), 0f),
            ),
            maxLines = 1,
        )
    }
}

/** [Bd.amount] 的一个字体像素 */
private val AMOUNT_FONT_PIXEL = (2f / 3f).dp

// 这个字体的小数点占 6 个像素宽，原版只占 2 个：把小数点与它前一个字符各收紧 2 个像素
private val NARROW = SpanStyle(letterSpacing = (-2f / 9f).em)

private fun narrowDots(text: String) = buildAnnotatedString {
    text.forEachIndexed { index, char ->
        if (char == '.' || text.getOrNull(index + 1) == '.') withStyle(NARROW) { append(char) } else append(char)
    }
}

/** 单行搜索框，右键清空。[help] 是悬停时的说明，开始输入后收起 */
@Composable
fun BdSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    help: (@Composable ColumnScope.() -> Unit)? = null,
) = BdTextField(value, onValueChange, placeholder, modifier, BdIcons.Search, help)

/** 单行文本框：外观与右键清空同 [BdSearchField]，[icon] 为 null 时左侧没有图标 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: BdPixelIcon? = null,
    help: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    BdTooltip(help ?: {}, modifier, enabled = help != null && !focused) {
        BasicTextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth().height(18.dp).hoverable(interaction).onPointerEvent(
                PointerEventType.Press,
                PointerEventPass.Initial,
            ) {
                // 先于文本框处理并吞掉右键
                if (it.buttons.isSecondaryPressed) {
                    onValueChange("")
                    it.changes.forEach { change -> change.consume() }
                }
            },
            singleLine = true,
            textStyle = Bd.body.copy(color = colors[BdColors.text]),
            interactionSource = interaction,
            cursorBrush = SolidColor(colors[BdColors.accent]),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth()
                        .fillMaxHeight()
                        .bdInset(
                            colors,
                            edge = when {
                                focused -> colors[BdColors.accent]
                                hovered -> colors[BdColors.lineStrong]
                                else -> null
                            },
                        )
                        .padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        BdIcon(icon, color = if (focused) colors[BdColors.accent] else colors[BdColors.faint])
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) OreText(placeholder, color = colors[BdColors.faint], maxLines = 1)
                        inner()
                    }
                    if (value.isNotEmpty()) {
                        Spacer(Modifier.width(3.dp))
                        BdGlyphButton(BdIcons.Close, null, { onValueChange("") }, size = 10.dp)
                    }
                }
            },
        )
    }
}

/**
 * 多行文本框：外观同 [BdTextField]，至少 [minLines] 行，超过 [maxLines] 行时在框内滚动。
 * 右键是剪切、复制、粘贴的菜单；[help] 是悬停时的说明
 */
@Composable
fun BdTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
    maxLines: Int = 8,
    help: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    BdTooltip(help ?: {}, modifier, enabled = help != null && !focused) {
        BasicTextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth().hoverable(interaction),
            textStyle = Bd.body.copy(color = colors[BdColors.text]),
            minLines = minLines,
            maxLines = maxLines,
            interactionSource = interaction,
            cursorBrush = SolidColor(colors[BdColors.accent]),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth()
                        .bdInset(
                            colors,
                            edge = when {
                                focused -> colors[BdColors.accent]
                                hovered -> colors[BdColors.lineStrong]
                                else -> null
                            },
                        )
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                ) {
                    if (value.isEmpty()) OreText(placeholder, color = colors[BdColors.faint])
                    inner()
                }
            },
        )
    }
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
            .bdRaised(
                colors,
                fill = if ((hovered || active) && enabled) colors[BdColors.surface] else colors[BdColors.window],
                edge = if ((hovered || active) && enabled) colors[BdColors.accent] else null,
            )
            .padding(horizontal = 5.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private data class BdSegmentBounds(val left: Dp, val width: Dp)

/** 互斥的模式：选中框随选项切换平滑移动 */
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
    val segments = remember(options) { mutableStateMapOf<Int, BdSegmentBounds>() }
    Box(
        modifier
            .height(15.dp)
            .bdInset(colors)
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
            // 选中框不参与轨道测量；轨道尺寸只由实际选项决定。
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier.absoluteOffset(x = left)
                        .width(width)
                        .fillMaxHeight()
                        .bdRaised(colors, fill = colors[BdColors.surface], edge = colors[BdColors.accent])
                )
            }
        }
        Layout(
            content = {
                options.forEachIndexed { index, option ->
                    val interaction = remember { MutableInteractionSource() }
                    val hovered by interaction.collectIsHoveredAsState()
                    val chosen = index == selected
                    Box(
                        Modifier.hoverable(interaction, enabled)
                            .bdClickable(interaction, enabled = enabled && !chosen) { onSelect(index) }
                            .onGloballyPositioned { position ->
                                segments[index] = with(density) {
                                    BdSegmentBounds(position.positionInParent().x.toDp(), position.size.width.toDp())
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        OreText(
                            option,
                            color =
                                when {
                                    chosen -> colors[BdColors.accentDeep]
                                    hovered && enabled -> colors[BdColors.text]
                                    else -> colors[BdColors.muted]
                                },
                            maxLines = 1,
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxHeight(),
        ) { measurables, constraints ->
            val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
            val text = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
            val padding = SEGMENT_PADDING.dp.roundToPx() * 2
            // 不占满的分段放不下时也收进可用的宽度
            val available = constraints.maxWidth.takeIf {
                constraints.hasBoundedWidth && (fill || text.sum() + padding * text.size > it)
            }
            val widths = segmentWidths(text, padding, available)
            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(
                    constraints.copy(minWidth = widths[index], maxWidth = widths[index], minHeight = height)
                )
            }
            layout(widths.sum(), placeables.maxOfOrNull { it.height } ?: height) {
                var x = 0
                for (placeable in placeables) {
                    placeable.placeRelative(x, 0)
                    x += placeable.width
                }
            }
        }
    }
}

/** 分段文字两侧各留的空位 */
private const val SEGMENT_PADDING = 5

/**
 * 各段的宽度，[text] 是各段文字的宽度。[available] 为 null 时每段是文字加两侧的 [padding]；
 * 否则占满 [available]：各段等宽，有文字放不下时改为各段按自己的文字宽度，再平分余下的空位
 */
private fun segmentWidths(text: List<Int>, padding: Int, available: Int?): List<Int> {
    if (available == null) return text.map { it + padding }
    val count = text.size
    if (count == 0) return emptyList()
    val total = text.sum()
    val widths =
        when {
            text.all { (it + padding) * count <= available } -> MutableList(count) { available / count }
            total <= available -> MutableList(count) { text[it] + (available - total) / count }
            // 文字的总宽就超出时按比例收窄
            else -> MutableList(count) { text[it] * available / total }
        }
    // 取整余下的几个像素给最后一段
    widths[count - 1] += available - widths.sum()
    return widths
}

/** 开关：凹槽里一块凸起的滑块，与滚动条的滑块相同；开启时滑到右侧并换成强调色 */
@Composable
fun BdToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val pointerOver by interaction.collectIsHoveredAsState()
    val hovered = pointerOver && enabled
    val motion = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
    val progress by animateFloatAsState(if (checked) 1f else 0f, motion, label = "BD switch")
    Box(
        Modifier.size(20.dp, 10.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .hoverable(interaction, enabled)
            .bdClickable(interaction, enabled = enabled) { onCheckedChange(!checked) }
            .bdInset(
                colors,
                fill = lerp(colors[BdColors.sunken], colors[BdColors.accentSoft], progress),
                edge = if (hovered) colors[BdColors.lineStrong] else null,
            )
            .padding(1.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val on = colors[BdColors.accent]
        Box(
            Modifier.offset(x = 10.dp * progress)
                .size(8.dp)
                .bdKnob(
                    fill = lerp(colors[BdColors.window], on, progress),
                    light = lerp(colors[BdColors.bevelLight], lerp(on, Color.White, .4f), progress),
                    dark = lerp(colors[BdColors.bevelDark], lerp(on, Color.Black, .4f), progress),
                )
        )
    }
}

/** 一行设置：左侧标题与说明，右侧控件 */
@Composable
fun BdSettingRow(title: String, description: String? = null, control: @Composable () -> Unit) {
    val colors = Bd.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            OreText(title, color = colors[BdColors.text], maxLines = 1)
            if (description != null) OreText(
                description,
                color = colors[BdColors.faint],
                style = Bd.caption,
                maxLines = 2
            )
        }
        Spacer(Modifier.width(4.dp))
        control()
    }
}

/**
 * 一组互斥模式：标题在上，下面是分段按钮；[cycle] 为真时改为左右切换。
 * [description] 是当前选项的说明
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
        OreText(title, color = colors[BdColors.text], maxLines = 1)
        Spacer(Modifier.height(2.dp))
        if (cycle) BdCycler(options, selected, enabled, onSelect)
        else BdSegmented(options, selected, onSelect, Modifier.fillMaxWidth(), enabled = enabled, fill = true)
        if (description != null) {
            Spacer(Modifier.height(2.dp))
            OreText(description, color = colors[BdColors.faint], style = Bd.caption, maxLines = 2)
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
            .bdInset(colors)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BdGlyphButton(
            BdIcons.ChevronLeft,
            null,
            { onSelect((selected - 1).mod(options.size)) },
            enabled = enabled,
            size = 13.dp,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            OreText(
                options.getOrElse(selected) { "" },
                color = if (enabled) colors[BdColors.accentDeep] else colors[BdColors.faint],
                maxLines = 1
            )
        }
        BdGlyphButton(
            BdIcons.ChevronRight,
            null,
            { onSelect((selected + 1).mod(options.size)) },
            enabled = enabled,
            size = 13.dp,
        )
    }
}

/** 纵向内容：放不下时可滚动，右侧出现滚动条 */
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
            Modifier.fillMaxWidth().padding(end = if (scrollable) (SCROLLBAR_WIDTH + 2).dp else 0.dp)
                .verticalScroll(scroll),
            verticalArrangement = verticalArrangement,
            content = content,
        )
        // 滚动条不参与测量，不会把容器撑到最大高度
        if (scrollable) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopEnd) {
                BdScrollbar(scroll, Modifier.width(SCROLLBAR_WIDTH.dp).fillMaxHeight())
            }
        }
    }
}

/** 滚动条的宽度；内容与它之间另留 2 */
const val SCROLLBAR_WIDTH = 7

/** 滑块的长度：像原版一样固定，不随内容多少变化 */
private const val SCROLLBAR_KNOB = 13

/** 按行滚动的滚动条：[first] 是第一个可见行，[visible] 与 [total] 为可见行数与总行数 */
@Composable
fun BdScrollbar(first: Int, visible: Int, total: Int, onScrollTo: (Int) -> Unit, modifier: Modifier = Modifier) {
    val maxFirst = (total - visible).coerceAtLeast(0)
    val latest by rememberUpdatedState(onScrollTo)
    BdScrollbarTrack(
        position = if (maxFirst > 0) first.toFloat() / maxFirst else 0f,
        canScroll = maxFirst > 0,
        onScrollTo = { latest((it * maxFirst).roundToInt()) },
        modifier = modifier,
    )
}

/** 跟随 [state] 的滚动条，与按行滚动的外观相同 */
@Composable
fun BdScrollbar(state: ScrollState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    // 第一次布局前 maxValue 是表示未知的 Int.MAX_VALUE
    val maximum = if (state.maxValue == Int.MAX_VALUE) 0 else state.maxValue
    BdScrollbarTrack(
        position = if (maximum > 0) state.value.toFloat() / maximum else 0f,
        canScroll = maximum > 0,
        onScrollTo = { scope.launch { state.scrollTo((it * maximum).roundToInt()) } },
        modifier = modifier,
    )
}

/**
 * 两种滚动条共用的外观与操作：凹槽里一块凸起的机壳滑块，中间三道防滑纹，像原版的滚动条。
 * 点击或拖动轨道时滑块中心跳到指针处，[onScrollTo] 收到 0 到 1 之间的位置。
 * 滑块在绘制时按轨道的实际高度算出：浮层里的子组合布局不支持固有尺寸的测量，不能用 BoxWithConstraints
 */
@Composable
private fun BdScrollbarTrack(position: Float, canScroll: Boolean, onScrollTo: (Float) -> Unit, modifier: Modifier) {
    val colors = Bd.colors
    val latest by rememberUpdatedState(onScrollTo)
    val knob = with(LocalDensity.current) { SCROLLBAR_KNOB.dp.toPx() }
    fun knobOf(track: Float) = minOf(knob, track)
    fun positionAt(y: Float, track: Float): Float {
        val length = knobOf(track)
        return ((y - length / 2f) / (track - length).coerceAtLeast(1f)).coerceIn(0f, 1f)
    }
    Box(
        modifier.bdInset(colors).padding(1.dp)
            .pointerInput(Unit) { detectTapGestures { latest(positionAt(it.y, size.height.toFloat())) } }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ -> latest(positionAt(change.position.y, size.height.toFloat())) }
            }
            .drawBehind {
                if (!canScroll) return@drawBehind
                val p = 1.dp.toPx()
                val w = size.width
                val length = knobOf(size.height)
                // 滑块停在整像素上
                val top = ((size.height - length) * position.coerceIn(0f, 1f) / p).roundToInt() * p
                translate(top = top) {
                    val face = colors[BdColors.window]
                    knob(Offset.Zero, Size(w, length), face, colors[BdColors.bevelLight], colors[BdColors.bevelDark])
                    // 防滑纹：中间三道暗线
                    val middle = (length / p).toInt() / 2 * p
                    for (offset in -2..2 step 2) {
                        drawRect(colors[BdColors.outline], Offset(p, middle + offset * p), Size(w - 2 * p, p))
                    }
                }
            }
    )
}

/** 停止输入多久后自动提交 */
private const val NUMBER_COMMIT_MILLIS = 800L

/** 输入框最多显示的位数，更长的数字在框内滚动 */
private const val NUMBER_VISIBLE_DIGITS = 10

/**
 * 整数编辑器：减号、可输入的数字与加号。按钮、方向键与滚轮立即改值，按住 Shift 或 Ctrl 时每次改 10 或 100；
 * 输入的数字在回车、失去焦点或停止输入片刻后提交，Esc 放弃
 */
@Composable
fun BdNumberEditor(value: Int, onValueChange: (Int) -> Unit, range: IntRange, enabled: Boolean = true) =
    BdNumberEditor(value.toLong(), { onValueChange(it.toInt()) }, range.first.toLong()..range.last.toLong(), enabled)

/** 与 Int 版相同，取值范围可以超出 Int */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BdNumberEditor(value: Long, onValueChange: (Long) -> Unit, range: LongRange, enabled: Boolean = true) {
    val colors = Bd.colors
    val focus = LocalFocusManager.current
    var draft by remember { mutableStateOf(value.toString()) }
    var dirty by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    // 值从外部变化时同步进来，正在输入时不打断
    LaunchedEffect(value, enabled) {
        if (!focused || !dirty || !enabled) {
            draft = value.toString()
            dirty = false
        }
    }
    fun clamp(number: Long) = number.coerceIn(range.first, range.last)

    // 位数超出 Long 的数字按 Long 的上下限处理，提交时再收进范围
    fun parse(text: String): Long? {
        text.toLongOrNull()?.let { return it }
        val negative = text.startsWith('-')
        val body = if (negative) text.drop(1) else text
        return if (body.isNotEmpty() && body.all(Char::isDigit)) (if (negative) Long.MIN_VALUE else Long.MAX_VALUE) else null
    }

    fun set(next: Long) {
        draft = next.toString()
        dirty = false
        if (next != value) onValueChange(next)
    }

    fun commit() {
        if (enabled && dirty) set(parse(draft)?.let(::clamp) ?: value)
    }

    fun step(direction: Int, modifiers: PointerKeyboardModifiers) {
        if (!enabled) return
        val amount = if (modifiers.isCtrlPressed) 100L else if (modifiers.isShiftPressed) 10L else 1L
        val current = clamp(parse(draft) ?: value)
        // 到边界时停在边界上，不会溢出
        val next =
            if (direction > 0) (if (current > range.last - amount) range.last else current + amount)
            else (if (current < range.first + amount) range.first else current - amount)
        set(next)
    }
    LaunchedEffect(draft, dirty) {
        if (dirty) {
            delay(NUMBER_COMMIT_MILLIS)
            commit()
        }
    }
    val invalid = dirty && parse(draft)?.let { it in range } != true
    val digits = maxOf(range.first.toString().length, range.last.toString().length)
    // 按钮的点击里拿不到修饰键，按下时先记下
    var pressed by remember { mutableStateOf(PointerKeyboardModifiers()) }
    val notePress = Modifier.onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
        pressed = it.keyboardModifiers
    }
    Row(
        Modifier.height(15.dp)
            .bdInset(
                colors,
                edge = when {
                    invalid -> colors[BdColors.danger]
                    focused -> colors[BdColors.accent]
                    else -> null
                },
            )
            .padding(1.dp)
            .alpha(if (enabled) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BdGlyphButton(
            BdIcons.Minus,
            null,
            { step(-1, pressed) },
            notePress,
            enabled = enabled && value > range.first,
            size = 13.dp,
        )
        BasicTextField(
            draft,
            { input ->
                val negative = range.first < 0 && input.startsWith('-')
                val body = if (negative) input.drop(1) else input
                if (body.length <= digits && body.all(Char::isDigit)) {
                    draft = input
                    dirty = true
                }
            },
            Modifier.width((minOf(digits, NUMBER_VISIBLE_DIGITS) * 6 + 6).dp)
                .onFocusChanged {
                    if (focused && !it.isFocused) commit()
                    focused = it.isFocused
                }
                .onPreviewKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter,
                        Key.NumPadEnter -> {
                            commit()
                            focus.clearFocus()
                            true
                        }

                        Key.Escape -> {
                            draft = value.toString()
                            dirty = false
                            focus.clearFocus()
                            true
                        }

                        Key.DirectionUp,
                        Key.DirectionDown -> {
                            step(
                                if (event.key == Key.DirectionUp) 1 else -1,
                                PointerKeyboardModifiers(
                                    isCtrlPressed = event.isCtrlPressed,
                                    isShiftPressed = event.isShiftPressed
                                ),
                            )
                            true
                        }

                        else -> false
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val delta = event.changes.sumOf { it.scrollDelta.y.toDouble() }
                    if (enabled && delta != 0.0) {
                        step(if (delta < 0) 1 else -1, event.keyboardModifiers)
                        event.changes.forEach { it.consume() }
                    }
                },
            enabled = enabled,
            singleLine = true,
            textStyle = Bd.body.copy(color = colors[BdColors.text], textAlign = TextAlign.Center),
            cursorBrush = SolidColor(colors[BdColors.accent]),
        )
        BdGlyphButton(
            BdIcons.Plus,
            null,
            { step(1, pressed) },
            notePress,
            enabled = enabled && value < range.last,
            size = 13.dp,
        )
    }
}
