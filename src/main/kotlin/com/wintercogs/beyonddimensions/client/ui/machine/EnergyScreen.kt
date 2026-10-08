package com.wintercogs.beyonddimensions.client.ui.machine

import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode
import com.wintercogs.beyonddimensions.common.menu.NetEnergyMenu
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * 维度网络能量通道：网络的存量与收支、近半分钟的收支曲线与玩家背包；设置页是弹出模式与红石控制。
 * 网络默认的容量极大，填充比例看不出变化，所以这里展示收支的走势，以及按当前速率多久后耗尽或充满。
 */
class EnergyScreen(menu: NetEnergyMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<NetEnergyMenu, EnergyState, EnergyAction>(menu, title) {
    private val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_ENERGY_PATHWAY.get()))
    private val playerSlots = menu.playerSlotIds()
    private val text = EnergyText(title.string)
    private val history = FlowHistory()

    override fun inventoryTick() {
        super.inventoryTick()
        history.record(container.energyRate())
    }

    override fun snapshot(): EnergyState {
        val stored = container.energyStored()
        val capacity = container.energyCapacity()
        val limited = capacity != Long.MAX_VALUE
        val rate = history.recent()
        val bars = history.bars()
        return EnergyState(
            pop = container.popMode() == PopMode.OPEN,
            redstone = container.redStoneMode(),
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
            outlook = outlook(stored, capacity, rate),
            flow = bars,
            peak = tr("ui.beyonddimensions.energy.peak", formatRate(bars.maxOfOrNull { abs(it) }?.toDouble() ?: 0.0)),
        )
    }

    /** 按最近一秒的平均速率估计多久后耗尽或充满；要一千天以上时不再估计 */
    private fun outlook(stored: Long, capacity: Long, rate: Double): Outlook? {
        val (key, ticks) =
            when {
                rate < 0 && stored > 0 -> "ui.beyonddimensions.energy.empty_in" to stored / -rate
                rate > 0 && capacity != Long.MAX_VALUE && capacity > stored ->
                    "ui.beyonddimensions.energy.full_in" to (capacity - stored) / rate

                else -> return null
            }
        val seconds = ceil(ticks / 20).toLong()
        val duration =
            when {
                seconds < 60 -> tr("ui.beyonddimensions.time.seconds", seconds)
                seconds < 3600 -> tr("ui.beyonddimensions.time.minutes", seconds / 60)
                seconds < 86400 -> tr("ui.beyonddimensions.time.hours", seconds / 3600, seconds % 3600 / 60)
                seconds < 1000L * 86400 -> tr("ui.beyonddimensions.time.days", seconds / 86400)
                else -> return null
            }
        return Outlook(tr(key, duration), draining = rate < 0)
    }

    override fun handle(action: EnergyAction) {
        when (action) {
            is EnergyAction.SetPop -> container.requestOutput((if (action.open) PopMode.OPEN else PopMode.STOP).ordinal)
            is EnergyAction.SetRedstone -> container.requestRedstone(action.mode.ordinal)
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
                rail = {
                    BdRailTab(text.title, BdGlyphs.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    InjectedTabs(pages, page)
                    Spacer(Modifier.weight(1f))
                    BdRailTab(text.settings, OreGlyph.Gear.art, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
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
                    BdSettingRow(text.pop, text.popHint) {
                        BdToggle(state.pop, { send(EnergyAction.SetPop(it)) }, enabled = settingsOpen)
                    }
                    BdModeSetting(text.redstone, text.redstoneOptions, state.redstone.ordinal, enabled = settingsOpen) {
                        send(EnergyAction.SetRedstone(RedStoneControlMode.entries[it]))
                    }
                }
            }
        }
    }
}

/** 估计的耗尽或充满时间；[draining] 为 true 时是耗尽 */
data class Outlook(val label: String, val draining: Boolean)

data class EnergyState(
    val pop: Boolean,
    val redstone: RedStoneControlMode,
    val stored: Long,
    /** 最近一秒平均每刻的变化，正数为流入 */
    val rate: Double,
    val capacity: String,
    /** 已用的比例，容量有上限时才有 */
    val used: String?,
    val outlook: Outlook?,
    /** 收支曲线每一格平均每刻的变化，从旧到新 */
    val flow: List<Float>,
    val peak: String,
)

sealed interface EnergyAction {
    data class SetPop(val open: Boolean) : EnergyAction

    data class SetRedstone(val mode: RedStoneControlMode) : EnergyAction
}

private class EnergyText(val title: String) {
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val network = tr("ui.beyonddimensions.energy.network")
    val unlimited = tr("ui.beyonddimensions.energy.unlimited")
    val window = tr("ui.beyonddimensions.energy.window", FLOW_BARS * BAR_TICKS / 20)
    val pop = tr("ui.beyonddimensions.machine.pop")
    val popHint = tr("ui.beyonddimensions.energy.pop.hint")
    val redstone = tr("ui.beyonddimensions.machine.redstone")
    val redstoneOptions =
        RedStoneControlMode.entries.map { tr("ui.beyonddimensions.machine.redstone.${it.name.lowercase()}") }
}

/** 收支曲线的格数，每格汇总的刻数 */
private const val FLOW_BARS = 52
private const val BAR_TICKS = 10

/** 每刻记下网络能量的变化，供收支曲线与平均速率使用；只保留曲线覆盖的那段时间 */
private class FlowHistory {
    private val samples = LongArray(FLOW_BARS * BAR_TICKS)
    private var recorded = 0L

    fun record(rate: Long) {
        samples[(recorded % samples.size).toInt()] = rate
        recorded++
    }

    private fun sample(tick: Long) = samples[(tick % samples.size).toInt()]

    /** 最近一秒平均每刻的变化 */
    fun recent(): Double {
        val count = minOf(recorded, 20L)
        if (count == 0L) return 0.0
        var sum = 0.0
        for (tick in recorded - count until recorded) sum += sample(tick)
        return sum / count
    }

    /** 每格平均每刻的变化，从旧到新；最新的一格可能还没有满 */
    fun bars(): List<Float> {
        if (recorded == 0L) return emptyList()
        val newest = (recorded - 1) / BAR_TICKS
        return (maxOf(0L, newest - FLOW_BARS + 1)..newest).map { bar ->
            val start = bar * BAR_TICKS
            val end = minOf(start + BAR_TICKS, recorded)
            var sum = 0.0
            for (tick in start until end) sum += sample(tick)
            (sum / (end - start)).toFloat()
        }
    }
}

/** 速率的大小：小于 100 时保留一位小数 */
private fun formatRate(rate: Double): String {
    val magnitude = abs(rate)
    return if (magnitude < 100) String.format(Locale.ROOT, "%.1f", magnitude).removeSuffix(".0")
    else formatReadout(magnitude.roundToLong())
}

/** 网络能量：存量与速率、容量与已用比例、收支曲线（悬停时显示峰值），以及曲线的时间范围与预计时间 */
@Composable
private fun EnergyReadout(state: EnergyState, text: EnergyText) {
    val colors = Bd.colors
    BdSectionLabel(text.network) { OreText("FE", color = colors[BdColors.faint], style = Bd.caption) }
    Spacer(Modifier.height(5.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        BdTooltip(formatExact(state.stored) + " FE") {
            OreText(formatReadout(state.stored), color = colors[BdColors.text], style = Bd.title, maxLines = 1)
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
                OreIcon(
                    if (state.rate > 0) OreGlyph.ArrowUp else OreGlyph.ArrowDown,
                    Modifier.size(6.dp),
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
    BdTooltip(state.peak, Modifier.fillMaxWidth()) { FlowChart(state.flow, Modifier.fillMaxWidth().height(39.dp)) }
    Spacer(Modifier.height(2.dp))
    Row(Modifier.fillMaxWidth()) {
        OreText(text.window, color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
        Spacer(Modifier.weight(1f))
        state.outlook?.let {
            OreText(
                it.label,
                color = if (it.draining) colors[BdColors.warning] else colors[BdColors.online],
                style = Bd.caption,
                maxLines = 1
            )
        }
    }
}

/** 每格的宽度，含右侧一格空隙 */
private const val BAR_PITCH = 3

/**
 * 收支曲线：中线以上为流入、以下为流出，按曲线里的最大值缩放，最新的一格在最右侧，越旧越淡。
 * 两条点线标出峰值的一半。按格绘制，一格一像素。
 */
@Composable
private fun FlowChart(bars: List<Float>, modifier: Modifier) {
    val colors = Bd.colors
    Canvas(modifier.background(colors[BdColors.sunken]).border(1.dp, colors[BdColors.line]).padding(2.dp)) {
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
