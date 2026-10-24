package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.wintercogs.beyonddimensions.api.ui.page.BdPageSize
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** 窗口左侧页签竖条的宽度 */
const val SIDE_RAIL_WIDTH = 24

/** 页面四周的留白，横向两侧相同 */
const val PAGE_PADDING_X = 8
const val PAGE_PADDING_TOP = 7
const val PAGE_PADDING_BOTTOM = 8

private const val SWITCH_MILLIS = 150
private const val SWITCH_RISE_DP = 6

/** 窗口变到另一页大小的时长 */
private const val RESIZE_MILLIS = 180

/** 当前页的大小；主页面与各页在显示时写入，主页面据此决定自己是否撑开窗口 */
@Stable
private class BdPageFrame {
    var current by mutableStateOf<BdPageSize>(BdPageSize.Main)

    /** 主页面上一次排版的高度（像素），隐藏期间也照常更新 */
    var mainHeight = 0
}

private val LocalBdPageFrame = compositionLocalOf<BdPageFrame?> { null }

/** 主页面连同留白的宽度；未指定时按主页面的内容 */
private val LocalBdMainWidth = staticCompositionLocalOf { Dp.Unspecified }

private val pagePadding =
    Modifier.padding(
        start = PAGE_PADDING_X.dp,
        end = PAGE_PADDING_X.dp,
        top = PAGE_PADDING_TOP.dp,
        bottom = PAGE_PADDING_BOTTOM.dp
    )

/**
 * BD 的标准窗口：顶部是 [header]，内容区左侧是页签竖条，右侧是 [pages]。
 *
 * 竖条上方是 [tabs]，页签多到放不下时这一段可以用滚轮滚动；[pinned] 固定在竖条底部，始终可见。
 * [width] 是主页面打开时窗口的宽度，不指定时由主页面的内容撑开；打开另定了大小（[BdPageSize]）的页时，
 * 窗口以动画变到那一页的大小。
 * 通常有两页：[BdMainPage] 与设置用的 [BdTabPage]，主页面的页签在上方，设置的页签固定在底部：
 * ```
 * var settingsOpen by remember { mutableStateOf(false) }
 * BdTabbedWindow(
 *     modifier,
 *     width = 200.dp,
 *     header = { BdHeader(icon, title, ::requestClose) },
 *     tabs = { BdRailTab(title, BdIcons.Main, selected = !settingsOpen) { settingsOpen = false } },
 *     pinned = { BdRailTab(settings, BdIcons.Settings, selected = settingsOpen) { settingsOpen = true } },
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
    tabs: @Composable ColumnScope.() -> Unit,
    pinned: @Composable ColumnScope.() -> Unit = {},
    width: Dp = Dp.Unspecified,
    pages: @Composable BoxScope.() -> Unit,
) {
    val colors = Bd.colors
    val frame = remember { BdPageFrame() }
    val mainWidth = if (width.isSpecified) width - SIDE_RAIL_WIDTH.dp else Dp.Unspecified
    Layout({
        Box { header() }
        Layout({
            // 页面嵌在屏幕里，切换时新页面从下方浮起，裁剪到内容区；内容区以动画变到当前页的大小
            CompositionLocalProvider(LocalBdPageFrame provides frame, LocalBdMainWidth provides mainWidth) {
                Box(
                    Modifier.padding(start = SIDE_RAIL_WIDTH.dp)
                        .bdScreen(
                            colors,
                            start = SCREEN_INSET_X.dp,
                            top = SCREEN_INSET_TOP.dp,
                            end = SCREEN_INSET_X.dp,
                            bottom = SCREEN_INSET_BOTTOM.dp,
                        )
                        .clipToBounds()
                        .animateScreenSize(),
                    content = pages,
                )
            }
            // 竖条在屏幕左侧的机壳上，与内容区一样高；页签靠右，选中的页签接进屏幕
            Box {
                Column(
                    Modifier.width(SIDE_RAIL_WIDTH.dp).fillMaxHeight().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    // 滚动只裁剪上下两端，选中页签伸进屏幕的一截不受影响
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        content = tabs,
                    )
                    pinned()
                }
            }
        }) { (screen, rail), constraints ->
            // 另定了大小的页比竖条上的页签矮时，内容区仍高到放下全部页签；但不比主页面高，
            // 页签多到主页面上也放不下时竖条照常滚动
            val railWidth = SIDE_RAIL_WIDTH.dp.roundToPx()
            val tabsHeight =
                if (frame.current == BdPageSize.Main) 0 else minOf(rail.minIntrinsicHeight(railWidth), frame.mainHeight)
            val page = screen.measure(constraints.copy(minWidth = 0, minHeight = tabsHeight.coerceAtMost(constraints.maxHeight)))
            val side = rail.measure(Constraints.fixed(railWidth, page.height))
            layout(page.width, page.height) {
                page.place(0, 0)
                side.place(0, 0)
            }
        }
    }, Modifier.centerContent().then(modifier).bdRaised(colors)) { (top, body), constraints ->
        // 窗口与页面一样宽：先量页面，再按页面的宽度排标题栏
        val headerHeight = top.minIntrinsicHeight(constraints.maxWidth)
        val bodyHeight =
            if (constraints.hasBoundedHeight) (constraints.maxHeight - headerHeight).coerceAtLeast(0) else Constraints.Infinity
        val content = body.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = bodyHeight))
        val windowWidth = content.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val title = top.measure(Constraints(windowWidth, windowWidth, 0, constraints.maxHeight))
        layout(windowWidth, (title.height + content.height).coerceIn(constraints.minHeight, constraints.maxHeight)) {
            title.place(0, 0)
            content.place(0, title.height)
        }
    }
}

/** 内容的大小一次只变了这么多，算作页面自己的动画的一步 */
private const val FOLLOW_STEP_DP = 8

/** 跟随中内容的大小在这段时间内又变了，仍算作同一段动画；比动画本身长，偶尔卡一帧也不会断开 */
private const val FOLLOW_NANOS = 250_000_000L

/**
 * 内容区的大小：一下变到新的大小时（例如换页）以动画过去，动画中再变就改换目标；
 * 内容小步改变大小时（例如页面自己的展开动画）直接跟随，不在后面追赶、裁掉内容
 */
@Composable
private fun Modifier.animateScreenSize(): Modifier {
    val scope = rememberCoroutineScope()
    val size = remember { ScreenSize(scope) }
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val content = IntSize(placeable.width, placeable.height)
        val shown = constraints.constrain(size.sizeFor(content, FOLLOW_STEP_DP.dp.roundToPx()))
        layout(shown.width, shown.height) { placeable.place(0, 0) }
    }
}

private class ScreenSize(private val scope: CoroutineScope) {
    private var animatable: Animatable<IntSize, AnimationVector2D>? = null
    private var target = IntSize.Zero
    private var changedAt = 0L
    private var following = false

    /** 内容是 [content] 大时内容区现在的大小；一次变化不超过 [step] 像素时跟随内容 */
    fun sizeFor(content: IntSize, step: Int): IntSize {
        val size = animatable ?: Animatable(content, IntSize.VectorConverter, IntSize(1, 1)).also {
            animatable = it
            target = content
        }
        if (content != target) {
            val now = System.nanoTime()
            val small = abs(content.width - target.width) <= step && abs(content.height - target.height) <= step
            val follow = !size.isRunning && (small || following && now - changedAt < FOLLOW_NANOS)
            following = follow
            changedAt = now
            target = content
            scope.launch {
                if (follow) size.snapTo(content) else size.animateTo(content, tween(RESIZE_MILLIS, easing = FastOutSlowInEasing))
            }
        }
        return if (following) content else size.value
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
 * 选中的页签是屏幕伸出的一截：底色与发光边接进右侧的屏幕。
 * 竖条上的开关按钮用 [active] 表示打开着，按钮仍可点击
 */
@Composable
fun BdRailTab(label: String, icon: BdPixelIcon, selected: Boolean, active: Boolean = false, onClick: () -> Unit) {
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
                        // 伸到屏幕的发光边上，盖住它与页签相接的一段。直接换成屏幕叠在机壳上的颜色，
                        // 屏幕半透明时发光边也不会从下面透出来
                        val reach = size.width + (SCREEN_INSET_X + 1).dp.toPx()
                        drawRect(
                            colors[BdColors.screen].compositeOver(colors[BdColors.window]),
                            Offset.Zero,
                            Size(reach, size.height),
                            blendMode = BlendMode.Src,
                        )
                        glowLine(colors, 0f, 0f, reach, horizontal = true, p)
                        glowLine(colors, 0f, size.height - p, reach, horizontal = true, p)
                        glowLine(colors, 0f, p, size.height - 2 * p, horizontal = false, p)
                    }
                    else Modifier.bdRaised(
                        colors,
                        fill = if (hovered || active) colors[BdColors.surface] else colors[BdColors.window],
                        edge = when {
                            active -> colors[BdColors.accent]
                            hovered -> colors[BdColors.activeEdge]
                            else -> null
                        },
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            BdIcon(
                icon,
                color =
                    when {
                        selected || active -> colors[BdColors.accentDeep]
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

/**
 * 主页面：显示时由它决定内容区的大小，切到其他页时淡出。
 * 打开的页有自己的大小时它不再撑开内容区，超出的部分被裁掉
 */
@Composable
fun BoxScope.BdMainPage(shown: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val progress = switchProgress(shown)
    val frame = LocalBdPageFrame.current
    val width = LocalBdMainWidth.current
    if (shown && frame != null) SideEffect { frame.current = BdPageSize.Main }
    ProvidePageVisibility(shown, progress) {
        Column(
            Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    frame?.mainHeight = placeable.height
                    val sizing = shown || frame == null || frame.current == BdPageSize.Main
                    layout(if (sizing) placeable.width else 0, if (sizing) placeable.height else 0) { placeable.place(0, 0) }
                }
                // 隐藏期间也按同样的宽度排版，切回来时不会跳动
                .then(if (width.isSpecified) Modifier.width(width) else Modifier)
                .graphicsLayer { alpha = progress.value }
                .then(pagePadding),
            content = content,
        )
    }
}

/**
 * 其他页面：默认与主页面一样大，[size] 可以另定大小；打开时盖住主页面并挡住它的点击，内容放不下时在页内滚动。
 * 隐藏期间不组合
 */
@Composable
fun BoxScope.BdTabPage(
    shown: Boolean,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(6.dp),
    size: BdPageSize = BdPageSize.Main,
    content: @Composable ColumnScope.() -> Unit,
) {
    val progress = switchProgress(shown)
    val frame = LocalBdPageFrame.current
    if (shown && frame != null) SideEffect { frame.current = size }
    if (!shown && progress.value == 0f) return
    val sized = when (size) {
        BdPageSize.Main -> Modifier.matchParentSize()
        is BdPageSize.Fixed -> Modifier.size(size.width.dp, size.height.dp)
        // 页内的滚动列会占满可用宽度，按内容的固有宽度取
        is BdPageSize.Content -> Modifier.widthIn(max = size.maxWidth?.dp ?: Dp.Infinity)
            .heightIn(max = size.maxHeight?.dp ?: Dp.Infinity)
            .width(IntrinsicSize.Max)
    }
    ProvidePageVisibility(shown, progress) {
        BdScrollColumn(
            sized
                .offset { IntOffset(0, ((1f - progress.value) * SWITCH_RISE_DP.dp.toPx()).roundToInt()) }
                .graphicsLayer { alpha = progress.value }
                // 没有控件的地方也接住指针，点击不会落到下层的主页面
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .then(pagePadding),
            verticalArrangement,
            content = content,
        )
    }
}

@Composable
private fun switchProgress(shown: Boolean) =
    animateFloatAsState(if (shown) 1f else 0f, tween(SWITCH_MILLIS, easing = FastOutSlowInEasing), label = "page")
