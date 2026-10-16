package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import com.wintercogs.beyonddimensions.common.menu.state.EnergyHistory
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 维度网络能量通道：网络的存量与收支、近半分钟的存量与收支柱状图与玩家背包；设置页是各面的输入输出、主动弹出与红石控制。
 * 网络的容量极大，填充比例看不出变化，所以展示存量的走势。柱状图的数据由服务端为每个网络记录，打开界面时就是完整的
 */
class EnergyScreen(menu: NetEnergyMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetEnergyMenu, EnergyState, EnergyAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get()))
    private val playerSlots = menu.playerSlotIds()
    private val text = EnergyText(title.string)
    private val sides = machineSidesView(menu.be)

    // 图表只在同步来新的一格时重算，每刻的快照沿用
    private var chartsSource = EnergyHistory.Bars.EMPTY
    private var charts = Charts(chartsSource)

    override fun snapshot(): EnergyState {
        val stored = container.energyStored()
        val capacity = container.energyCapacity()
        val limited = capacity != Long.MAX_VALUE
        val rate = container.energyRate()
        val history = container.energyBars()
        if (history !== chartsSource) {
            chartsSource = history
            charts = Charts(history)
        }
        return EnergyState(
            settings = machineSettingRows(container.machineSettings().snapshot(), sides),
            stored = stored,
            rate = rate,
            capacity = tr(
                "ui.beyonddimensions.energy.capacity",
                if (limited) formatReadout(capacity) else text.unlimited
            ),
            used =
                if (limited && capacity > 0)
                    tr(
                        "ui.beyonddimensions.energy.used",
                        String.format(Locale.ROOT, "%.0f%%", stored * 100.0 / capacity)
                    )
                else null,
            flow = charts.flow,
            peak = charts.peak,
            trend = charts.trend,
            range = charts.range,
        )
    }

    override fun handle(action: EnergyAction) {
        when (action) {
            is EnergyAction.Setting -> container.requestSetting(action.pick.setting, action.pick.value)
        }
    }

    @Composable
    override fun Content(state: EnergyState, slots: ComposeMenuSlots<NetEnergyMenu>) {
        val page = rememberPageSelection()
        val settingsOpen = page.settings
        BdScreenFrame {
            BdTabbedWindow(
                Modifier.width(SLOT_WINDOW_WIDTH.dp).then(slots.areaModifier()),
                header = { BdHeader(icon, text.title, ::requestClose) },
                tabs = {
                    BdRailTab(text.title, BdIcons.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                },
                pinned = {
                    BdRailTab(text.settings, BdIcons.Settings, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
                },
            ) {
                BdMainPage(page.main) {
                    EnergyReadout(state, text)
                    Spacer(Modifier.height(8.dp))
                    BdInventorySection(text.inventory, playerSlots, slots)
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) {
                    BdSectionLabel(text.settings)
                    BdMachineSettings(state.settings, settingsOpen) { send(EnergyAction.Setting(it)) }
                }
            }
        }
    }
}

data class EnergyState(
    val settings: List<MachineSettingRow>,
    val stored: Long,
    /** 最近一秒平均每刻的变化，正数为流入 */
    val rate: Double,
    val capacity: String,
    /** 已用的比例，容量有上限时才有 */
    val used: String?,
    /** 收支柱状图每一格平均每刻的变化，从旧到新 */
    val flow: List<Float>,
    val peak: String,
    /** 存量柱状图每一格结束时的存量，从旧到新 */
    val trend: List<Long>,
    /** 存量柱状图覆盖的最低与最高存量 */
    val range: String,
)

sealed interface EnergyAction {
    data class Setting(val pick: MachineSettingPick) : EnergyAction
}

private class EnergyText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val network = tr("ui.beyonddimensions.energy.network")
    val unlimited = tr("ui.beyonddimensions.energy.unlimited")
}

/** 每格的宽度：两列柱子加一列空隙 */
private const val BAR_PITCH = 3

/** 柱状图内部的宽度：页面内容宽减去两侧各 2 的内边距 */
private const val CHART_COLUMNS = SLOT_WINDOW_WIDTH - SIDE_RAIL_WIDTH - 2 * PAGE_PADDING_X - 4

/** 柱状图的格数，最旧与最新的一格正好贴住两边；服务端保留的格数不少于这些 */
private const val FLOW_BARS = (CHART_COLUMNS + 1) / BAR_PITCH

/** 由同步来的各格算出的两张柱状图，只取最新的 [FLOW_BARS] 格；在游戏线程构造 */
private class Charts(history: EnergyHistory.Bars) {
    /** 每格平均每刻的收支 */
    val flow: List<Float>
    val peak: String

    /** 每格结束时的存量 */
    val trend: List<Long>
    val range: String

    init {
        val flows = history.flows()
        val stored = history.stored()
        val first = maxOf(0, flows.size - FLOW_BARS)
        val flow = ArrayList<Float>(flows.size - first)
        val trend = ArrayList<Long>(flows.size - first)
        var peak = 0f
        var low = Long.MAX_VALUE
        var high = Long.MIN_VALUE
        for (i in first until flows.size) {
            val rate = flows[i].toFloat() / EnergyHistory.BAR_TICKS
            flow.add(rate)
            peak = maxOf(peak, abs(rate))
            trend.add(stored[i])
            low = minOf(low, stored[i])
            high = maxOf(high, stored[i])
        }
        this.flow = flow
        this.trend = trend
        this.peak = tr("ui.beyonddimensions.energy.peak", formatRate(peak.toDouble()))
        this.range =
            if (trend.isEmpty()) "" else tr("ui.beyonddimensions.energy.range", formatReadout(low), formatReadout(high))
    }
}

/** 速率的大小：小于 100 时保留一位小数 */
private fun formatRate(rate: Double): String {
    val magnitude = abs(rate)
    return if (magnitude < 100) String.format(Locale.ROOT, "%.1f", magnitude).removeSuffix(".0")
    else formatReadout(magnitude.roundToLong())
}

/** 网络能量：存量与速率、容量与已用比例、存量与收支柱状图 */
@Composable
private fun EnergyReadout(state: EnergyState, text: EnergyText) {
    val colors = Bd.colors
    BdSectionLabel(text.network)
    Spacer(Modifier.height(5.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        BdTooltip(formatExact(state.stored) + " FE") {
            Row {
                OreText(
                    formatReadout(state.stored),
                    Modifier.alignByBaseline(),
                    color = colors[BdColors.text],
                    style = Bd.title,
                    maxLines = 1
                )
                Spacer(Modifier.width(2.dp))
                OreText(
                    "FE",
                    Modifier.alignByBaseline(),
                    color = colors[BdColors.faint],
                    style = Bd.caption,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.weight(1f))
        val rateColor =
            when {
                state.rate > 0 -> colors[BdColors.online]
                state.rate < 0 -> colors[BdColors.danger]
                else -> colors[BdColors.faint]
            }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.rate != 0.0) {
                BdIcon(
                    if (state.rate > 0) BdIcons.ArrowUp else BdIcons.ArrowDown,
                    color = rateColor
                )
                Spacer(Modifier.width(2.dp))
            }
            OreText(formatRate(state.rate) + " FE/t", color = rateColor, style = Bd.caption, maxLines = 1)
        }
    }
    Spacer(Modifier.height(1.dp))
    Row(Modifier.fillMaxWidth()) {
        OreText(state.capacity, color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
        Spacer(Modifier.weight(1f))
        if (state.used != null) OreText(state.used, color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
    }
    Spacer(Modifier.height(4.dp))
    BdTooltip(state.range, Modifier.fillMaxWidth(), enabled = state.trend.isNotEmpty()) {
        TrendChart(state.trend, Modifier.fillMaxWidth().height(21.dp))
    }
    Spacer(Modifier.height(3.dp))
    BdTooltip(state.peak, Modifier.fillMaxWidth()) { FlowChart(state.flow, Modifier.fillMaxWidth().height(33.dp)) }
}

/**
 * 存量柱状图：每格是那时的存量，最新的一格在最右侧，越旧越淡。按这段时间的最低与最高存量缩放，
 * 最低的一格约占四分之一高（存量离零较近时从零算起），网络容量再大也看得出涨落
 */
@Composable
private fun TrendChart(values: List<Long>, modifier: Modifier) {
    val colors = Bd.colors
    Canvas(modifier.bdInset(colors).padding(2.dp)) {
        val cell = 1.dp.toPx()
        val columns = (size.width / cell).toInt()
        val rows = (size.height / cell).toInt()
        val high = values.maxOrNull() ?: return@Canvas
        if (high <= 0L) return@Canvas
        val low = values.min()
        // 先在整数里求差，存量极大时换成浮点也不丢掉涨落
        val range = (high - low).toDouble()
        val below = minOf(low.toDouble(), range / 3)
        val span = range + below
        values.asReversed().forEachIndexed { age, value ->
            val left = columns - (age + 1) * BAR_PITCH + 1
            if (left < 0) return@forEachIndexed
            val height =
                if (span <= 0.0) rows
                else (((value - low).toDouble() + below) / span * rows).roundToInt().coerceIn(1, rows)
            val color = colors[BdColors.accent].copy(alpha = 1f - 0.65f * age / FLOW_BARS)
            val top = (rows - height) * cell
            drawRect(color, Offset(left * cell, top), Size((BAR_PITCH - 1) * cell, height * cell))
            // 柱顶亮一行
            drawRect(
                lerp(color, colors[BdColors.text], 0.5f),
                Offset(left * cell, top),
                Size((BAR_PITCH - 1) * cell, cell)
            )
        }
    }
}

/**
 * 收支柱状图：中线以上为流入、以下为流出，按最大值缩放，最新的一格在最右侧，越旧越淡。
 * 两条点线标出峰值的一半
 */
@Composable
private fun FlowChart(bars: List<Float>, modifier: Modifier) {
    val colors = Bd.colors
    Canvas(modifier.bdInset(colors).padding(2.dp)) {
        val cell = 1.dp.toPx()
        val columns = (size.width / cell).toInt()
        // 中线上下各 half 行
        val half = ((size.height / cell).toInt() - 1) / 2
        val middle = half * cell
        for (x in 0 until columns step 4) {
            drawRect(colors[BdColors.line], Offset(x * cell, (half - half / 2) * cell), Size(cell, cell))
            drawRect(colors[BdColors.line], Offset(x * cell, (half + half / 2) * cell), Size(cell, cell))
        }
        drawRect(colors[BdColors.lineStrong], Offset(0f, middle), Size(columns * cell, cell))
        val peak = bars.maxOfOrNull { abs(it) } ?: 0f
        if (peak == 0f) return@Canvas
        bars.asReversed().forEachIndexed { age, value ->
            if (value == 0f) return@forEachIndexed
            val left = columns - (age + 1) * BAR_PITCH + 1
            if (left < 0) return@forEachIndexed
            val rows = (abs(value) / peak * half).toInt().coerceAtLeast(1)
            val base = if (value > 0) colors[BdColors.online] else colors[BdColors.danger]
            val color = base.copy(alpha = 1f - 0.65f * age / FLOW_BARS)
            val top = if (value > 0) middle - rows * cell else middle + cell
            drawRect(color, Offset(left * cell, top), Size((BAR_PITCH - 1) * cell, rows * cell))
            // 柱顶亮一行
            val cap = if (value > 0) top else top + (rows - 1) * cell
            drawRect(
                lerp(color, colors[BdColors.text], 0.5f),
                Offset(left * cell, cap),
                Size((BAR_PITCH - 1) * cell, cell)
            )
        }
    }
}
