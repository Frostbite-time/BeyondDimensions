package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdInventorySection
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_PITCH
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_WINDOW_WIDTH
import com.wintercogs.beyonddimensions.client.ui.base.playerSlotIds
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphs
import com.wintercogs.beyonddimensions.client.ui.kit.BdHeader
import com.wintercogs.beyonddimensions.client.ui.kit.BdMainPage
import com.wintercogs.beyonddimensions.client.ui.kit.BdModeSetting
import com.wintercogs.beyonddimensions.client.ui.kit.BdRailTab
import com.wintercogs.beyonddimensions.client.ui.kit.BdScreenFrame
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.BdSettingRow
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabPage
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabbedWindow
import com.wintercogs.beyonddimensions.client.ui.kit.BdToggle
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.AutoSortMode
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetFurnaceMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreText
import kotlin.math.ceil
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

/**
 * 网络熔炉（也用于高炉与烟熏炉）：九列熔炼位与每列的进度图标、两侧的输入与燃料标记、返还容器与燃料储备，以及玩家背包；
 * 设置页是弹出产物、接收输入、自动整理与红石控制。
 */
class FurnaceScreen(menu: NetFurnaceMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetFurnaceMenu, FurnaceState, FurnaceAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(menu.be?.blockState?.block ?: BDBlocks.NET_FURNACE_BLOCK.get()))
    private val slotIds = FurnaceSlots(menu)
    private val playerSlots = menu.playerSlotIds()
    private val text = FurnaceText(title.string)

    override fun snapshot() =
        FurnaceState(
            lanes = container.lanes().map { Lane(fraction(it.cooking(), it.cookingTotal()), fraction(it.burning(), it.burningTotal())) },
            pop = container.output() == PopMode.OPEN,
            popEditable = container.outputEditable(),
            receive = container.receive() == ReceiveMode.OPEN,
            receiveEditable = container.receiveEditable(),
            sorting = container.sorting() == AutoSortMode.OPEN,
            sortingEditable = container.sortingEditable(),
            redstone = container.redstone(),
            redstoneEditable = container.redstoneEditable(),
        )

    private fun fraction(value: Int, total: Int) = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f

    override fun handle(action: FurnaceAction) {
        when (action) {
            is FurnaceAction.SetPop -> container.requestOutput((if (action.on) PopMode.OPEN else PopMode.STOP).ordinal)
            is FurnaceAction.SetReceive -> container.requestReceive((if (action.on) ReceiveMode.OPEN else ReceiveMode.STOP).ordinal)
            is FurnaceAction.SetSorting -> container.requestSorting((if (action.on) AutoSortMode.OPEN else AutoSortMode.STOP).ordinal)
            is FurnaceAction.SetRedstone -> container.requestRedstone(action.mode.ordinal)
        }
    }

    @Composable
    override fun Content(state: FurnaceState, slots: ComposeMenuSlots<NetFurnaceMenu>) {
        var settingsOpen by remember { mutableStateOf(false) }
        slots.Interaction(enabled = !settingsOpen)
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(FURNACE_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                rail = {
                    BdRailTab(text.title, BdGlyphs.Main, selected = !settingsOpen) { settingsOpen = false }
                    Spacer(Modifier.weight(1f))
                    BdRailTab(text.settings, OreGlyph.Gear.art, selected = settingsOpen) { settingsOpen = true }
                },
            ) {
                BdMainPage(!settingsOpen) { Furnace(state, slotIds, playerSlots, text, slots) }
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdSettingRow(text.pop, text.popHint) {
                        BdToggle(state.pop, { send(FurnaceAction.SetPop(it)) }, settingsOpen && state.popEditable)
                    }
                    BdSettingRow(text.receive, text.receiveHint) {
                        BdToggle(state.receive, { send(FurnaceAction.SetReceive(it)) }, settingsOpen && state.receiveEditable)
                    }
                    BdSettingRow(text.sorting, text.sortingHint) {
                        BdToggle(state.sorting, { send(FurnaceAction.SetSorting(it)) }, settingsOpen && state.sortingEditable)
                    }
                    BdModeSetting(text.redstone, text.redstoneOptions, state.redstone.ordinal, settingsOpen && state.redstoneEditable) {
                        send(FurnaceAction.SetRedstone(RedStoneControlMode.entries[it]))
                    }
                }
            }
        }
    }
}

/** 一列熔炼位的熔炼进度与燃料余量，都在 0 到 1 之间 */
data class Lane(val cooking: Float, val burning: Float)

data class FurnaceState(
    val lanes: List<Lane>,
    val pop: Boolean,
    val popEditable: Boolean,
    val receive: Boolean,
    val receiveEditable: Boolean,
    val sorting: Boolean,
    val sortingEditable: Boolean,
    val redstone: RedStoneControlMode,
    val redstoneEditable: Boolean,
)

sealed interface FurnaceAction {
    data class SetPop(val on: Boolean) : FurnaceAction

    data class SetReceive(val on: Boolean) : FurnaceAction

    data class SetSorting(val on: Boolean) : FurnaceAction

    data class SetRedstone(val mode: RedStoneControlMode) : FurnaceAction
}

/** 熔炉各区的槽位编号 */
private class FurnaceSlots(menu: NetFurnaceMenu) {
    val inputFilters: List<Int> = menu.inputFilterSlotIds()
    val fuelFilters: List<Int> = menu.fuelFilterSlotIds()
    val inputs: List<Int> = menu.inputStorageSlotIds()
    val outputs: List<Int> = menu.outputStorageSlotIds()
    /** 燃料用尽后留下的容器，例如熔岩桶的空桶 */
    val fuelReturn: Int = menu.fuelReturnSlotIds().single()
    val fuelStorage: Int = menu.fuelStorageSlotIds().single()
}

private class FurnaceText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val input = tr("ui.beyonddimensions.furnace.input")
    val fuel = tr("ui.beyonddimensions.furnace.fuel")
    val smelting = tr("ui.beyonddimensions.furnace.smelting")
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.furnace.pop.hint")
    val receive = tr("ui.beyonddimensions.furnace.receive")
    val receiveHint = tr("ui.beyonddimensions.furnace.receive.hint")
    val sorting = tr("ui.beyonddimensions.furnace.sorting")
    val sortingHint = tr("ui.beyonddimensions.furnace.sorting.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions = RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}

private const val COLUMN_GAP = 4
/** 两侧各一列标记槽与列间距，加上中间九列的标准窗口宽度 */
private const val FURNACE_WINDOW_WIDTH = SLOT_WINDOW_WIDTH + 2 * (SLOT_PITCH + 1 + COLUMN_GAP)
/** 九列熔炼位加外圈细线的宽度 */
private const val LANES_WIDTH = 9 * SLOT_PITCH + 1
/** 进度图标按一格一像素绘制 */
private const val GLYPH = 16
/** 分组标题的行高：9sp 小字的行高 */
private const val LABEL = 11

/**
 * 参照改版前的网络熔炉：左列是八个输入标记，最下方是返还容器的槽位；右列是八个燃料标记，最下方是燃料储备；
 * 中间九列熔炼位，下方是背包。每列原料下方的箭头随熔炼进度由上往下点亮，产物下方的火焰随剩余燃料由上往下熄灭。
 */
@Composable
private fun Furnace(
    state: FurnaceState,
    ids: FurnaceSlots,
    playerSlots: List<Int>,
    text: FurnaceText,
    slots: ComposeMenuSlots<NetFurnaceMenu>,
) {
    // 有一列在熔炼或燃烧时才走动效的时钟，空闲时不额外刷新
    val clock = if (state.lanes.any { it.cooking > 0f || it.burning > 0f }) rememberFurnaceClock() else null
    Row {
        FlagColumn(text.input, ids.inputFilters, ids.fuelReturn, slots)
        Spacer(Modifier.width(COLUMN_GAP.dp))
        Column(Modifier.width(LANES_WIDTH.dp)) {
            BdSectionLabel(text.smelting)
            Spacer(Modifier.height(4.dp))
            Well { BdSlotGrid(slots, ids.inputs, 9) }
            LaneGlyphs(state.lanes) { lane, _ -> CookConduit(lane.cooking, clock) }
            Well { BdSlotGrid(slots, ids.outputs, 9) }
            LaneGlyphs(state.lanes) { lane, index -> Flames(lane.burning, index, clock) }
            Spacer(Modifier.height(6.dp))
            BdInventorySection(text.inventory, playerSlots, slots)
        }
        Spacer(Modifier.width(COLUMN_GAP.dp))
        FlagColumn(text.fuel, ids.fuelFilters, ids.fuelStorage, slots)
    }
}

/** 一列八个标记槽，上方是列名，最下方隔开一个槽位：标记槽与熔炼位上沿对齐，最下方的槽位与快捷栏底边对齐 */
@Composable
private fun FlagColumn(label: String, flags: List<Int>, last: Int, slots: ComposeMenuSlots<NetFurnaceMenu>) {
    // 中间一列：标题、两行槽位与两行图标、背包（标题、三行加 3 的间隔再一行）
    val lanes = LABEL + 4 + 2 * (SLOT_PITCH + 1) + 2 * GLYPH + 6 + LABEL + 4 + (4 * SLOT_PITCH + 3 + 1)
    val gap = lanes - (LABEL + 4 + (8 * SLOT_PITCH + 1) + (SLOT_PITCH + 1))
    Column {
        // 列名可以比一格宽，向两侧伸出
        Box(Modifier.width((SLOT_PITCH + 1).dp).height(LABEL.dp), contentAlignment = Alignment.Center) {
            OreText(label, Modifier.wrapContentWidth(unbounded = true), color = Bd.colors.muted, style = Bd.overline, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))
        Well { BdSlotGrid(slots, flags, 1) }
        Spacer(Modifier.height(gap.dp))
        Well { BdSlot(slots, last) }
    }
}

/** 槽位外的一圈细线 */
@Composable
private fun Well(content: @Composable () -> Unit) = Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) { content() }

/** 每列熔炼位一个图标，与上方的槽位对齐 */
@Composable
private fun LaneGlyphs(lanes: List<Lane>, glyph: @Composable (Lane, Int) -> Unit) {
    Row(Modifier.padding(start = 0.5.dp)) {
        lanes.forEachIndexed { index, lane ->
            Box(Modifier.size(SLOT_PITCH.dp, GLYPH.dp), contentAlignment = Alignment.Center) { glyph(lane, index) }
        }
    }
}

/** 动效的时钟：两秒一圈，从 0 走到 1。只在绘制时读取，动效只重绘、不重组 */
@Composable
private fun rememberFurnaceClock(): State<Float> =
    rememberInfiniteTransition(label = "furnace")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(CLOCK_MILLIS, easing = LinearEasing)), label = "clock")

private const val CLOCK_MILLIS = 2000

/** 导管的各行：第几行、从第几格开始、几格宽。上面四段竖管，下面是箭头尖，自上而下排列 */
private val CONDUIT =
    listOf(0, 1, 3, 4, 6, 7, 9, 10).map { Triple(it, 7, 4) } +
        listOf(Triple(12, 5, 8), Triple(13, 6, 6), Triple(14, 7, 4), Triple(15, 8, 2))

/**
 * 熔炼进度：分段的竖向导管接一个箭头尖，随进度由上往下点亮，颜色沿标志渐变由青到紫。
 * 熔炼时一道亮光沿点亮的部分向下流动，每秒一趟。按格绘制，一格一像素。
 */
@Composable
private fun CookConduit(progress: Float, clock: State<Float>?) {
    val colors = Bd.colors
    Canvas(Modifier.size(SLOT_PITCH.dp, GLYPH.dp)) {
        val cell = 1.dp.toPx()
        val lit = (progress.coerceIn(0f, 1f) * CONDUIT.size).toInt()
        // 亮光占两行，从点亮部分的上方进入、从下方离开
        val flow = if (clock != null && lit > 0) (clock.value * 2f % 1f) * (lit + 2) - 2 else Float.NaN
        CONDUIT.forEachIndexed { index, (row, from, width) ->
            var color = if (index < lit) lerp(colors.cyan, colors.violet, index / (CONDUIT.size - 1f)) else colors.lineStrong
            if (index < lit && index >= flow && index < flow + 2) color = lerp(color, colors.text, 0.6f)
            drawRect(color, Offset(from * cell, row * cell), Size(width * cell, cell))
        }
    }
}

/** 一簇火苗：左边第几格、几格宽、最高几行，以及起伏时最多矮几行 */
private class Tongue(val left: Int, val width: Int, val height: Int, val flicker: Int)

/** 照改版前的贴图：三簇竖向的火苗，中间一簇最高最宽，簇间各空一格 */
private val TONGUES = listOf(Tongue(3, 3, 9, 1), Tongue(7, 4, 12, 2), Tongue(12, 3, 9, 1))

/** 火焰区域的行数；剩余燃料按这些行由上往下熄灭 */
private const val FLAME_ROWS = 12

/** 每圈时钟里的动画帧数，即每秒十帧 */
private const val FLICKER_FRAMES = 20

/** 各帧火苗比最高时矮几行，以及火尖向左或向右偏一格；各簇、各列错开取用 */
private val FLICKER = intArrayOf(0, 1, 0, 2, 1, 0, 0, 1, 2, 1, 0, 1, 0, 0, 2, 1, 1, 0, 1, 0)
private val SWAY = intArrayOf(0, 1, 0, -1, 0, 0, 1, 0, -1, -1, 0, 1, 0, 0, -1, 0, 1, 1, 0, -1)

/**
 * 剩余燃料：三簇火苗，焰心、中焰与外焰三层颜色取自主题。燃烧时火苗高低起伏、火尖摆动，各簇与各列节奏错开；
 * 燃料用掉的部分由上往下变暗，剩得再少也亮一行。没有燃料时整团火焰暗着、不动。
 */
@Composable
private fun Flames(fuel: Float, lane: Int, clock: State<Float>?) {
    val colors = Bd.colors
    Canvas(Modifier.size(SLOT_PITCH.dp, GLYPH.dp)) {
        val cell = 1.dp.toPx()
        val burning = fuel > 0f && clock != null
        val frame = if (burning) (clock.value * FLICKER_FRAMES).toInt() else 0
        val lit = ceil(fuel.coerceIn(0f, 1f) * FLAME_ROWS).toInt()
        TONGUES.forEachIndexed { index, tongue ->
            val step = (frame + lane * 7 + index * 11) % FLICKER_FRAMES
            val drop = if (burning) FLICKER[step].coerceAtMost(tongue.flicker) else 0
            val sway = if (burning) SWAY[step] else 0
            val height = tongue.height - drop
            val core = (height + 1) / 2
            for (row in 0 until height) { // 第 0 行在最下面
                val fromTop = height - 1 - row
                // 越往上越窄，收成一格的火尖；收窄的几行随摆动偏向一侧，火尖偏得最多
                val width = minOf(tongue.width, 1 + fromTop * tongue.width / 3)
                val slack = tongue.width - width
                val start = tongue.left + ((slack + sway * (if (fromTop == 0) 2 else 1)) / 2).coerceIn(0, slack)
                for (x in start until start + width) {
                    val edge = x == start || x == start + width - 1
                    val layer =
                        when {
                            fromTop <= 2 || (edge && row > 0) -> colors.flameOuter
                            row < core && !edge -> colors.flameCore
                            else -> colors.flameMiddle
                        }
                    drawRect(
                        if (row < lit) layer else colors.lineStrong,
                        Offset(x * cell, (GLYPH - 1 - row) * cell),
                        Size(cell, cell),
                    )
                }
            }
        }
    }
}
