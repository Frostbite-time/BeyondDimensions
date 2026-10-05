package com.wintercogs.beyonddimensions.client.ui.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdController
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdPlayerInventory
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.LocalBdScreen
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphButton
import com.wintercogs.beyonddimensions.client.ui.kit.BdHeader
import com.wintercogs.beyonddimensions.client.ui.kit.BdModeSetting
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.BdSettingRow
import com.wintercogs.beyonddimensions.client.ui.kit.BdStatus
import com.wintercogs.beyonddimensions.client.ui.kit.BdToggle
import com.wintercogs.beyonddimensions.client.ui.kit.BdTone
import com.wintercogs.beyonddimensions.client.ui.kit.BdWindow
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import dev.compixel.forge.item.ItemIcon
import dev.compixel.ui.ore.theme.OreTheme
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreText
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ItemLike
import net.minecraft.world.item.ItemStack

// ---- 设置项的声明（游戏线程） ----

/** 机器的一项设置：读取与修改都在游戏线程进行 */
sealed interface Setting

/** 互斥的几种模式；[request] 的参数是选项下标 */
class ModeSetting(
    val title: String,
    val options: List<String>,
    val selected: () -> Int,
    val editable: () -> Boolean,
    val request: (Int) -> Boolean,
    /** 按当前选项给出的说明 */
    val describe: (Int) -> String? = { null },
    /** 以左右切换代替分段按钮，用于选项多或文字长的设置 */
    val cycle: Boolean = options.size > 4,
) : Setting

class ToggleSetting(
    val title: String,
    val description: String?,
    val checked: () -> Boolean,
    val editable: () -> Boolean,
    val request: (Boolean) -> Boolean,
) : Setting

class NumberSetting(
    val title: String,
    val description: String?,
    val value: () -> Int,
    val range: IntRange,
    val editable: () -> Boolean,
    val request: (Int) -> Boolean,
) : Setting

// ---- 界面快照 ----

sealed interface SettingView {
    val title: String
    val enabled: Boolean
}

data class ModeView(
    override val title: String,
    val description: String?,
    val options: List<String>,
    val selected: Int,
    override val enabled: Boolean,
    val cycle: Boolean,
) : SettingView

data class ToggleView(override val title: String, val description: String?, val checked: Boolean, override val enabled: Boolean) :
    SettingView

data class NumberView(
    override val title: String,
    val description: String?,
    val value: Int,
    val range: IntRange,
    override val enabled: Boolean,
) : SettingView

/** 机器页面在设置之外的读数 */
interface MachineReadout

data class MachineState(val ready: Boolean = false, val settings: List<SettingView> = emptyList(), val readout: MachineReadout? = null)

sealed interface MachineAction {
    data class Select(val index: Int, val option: Int) : MachineAction

    data class Toggle(val index: Int, val checked: Boolean) : MachineAction

    data class Number(val index: Int, val value: Int) : MachineAction
}

class MachineController(
    private val ready: () -> Boolean,
    private val settings: List<Setting>,
    private val readout: () -> MachineReadout? = { null },
) : BdController<MachineState, MachineAction> {
    /** 设置栏在首个快照之前就要占位，窗口宽度不随同步跳动 */
    val hasSettings = settings.isNotEmpty()

    override fun snapshot() = MachineState(ready(), settings.map(::view), readout())

    private fun view(setting: Setting): SettingView =
        when (setting) {
            is ModeSetting -> {
                val selected = setting.selected()
                ModeView(setting.title, setting.describe(selected), setting.options, selected, setting.editable(), setting.cycle)
            }
            is ToggleSetting -> ToggleView(setting.title, setting.description, setting.checked(), setting.editable())
            is NumberSetting -> NumberView(setting.title, setting.description, setting.value(), setting.range, setting.editable())
        }

    override fun handle(action: MachineAction) {
        when (action) {
            is MachineAction.Select -> (settings.getOrNull(action.index) as? ModeSetting)?.request?.invoke(action.option)
            is MachineAction.Toggle -> (settings.getOrNull(action.index) as? ToggleSetting)?.request?.invoke(action.checked)
            is MachineAction.Number ->
                (settings.getOrNull(action.index) as? NumberSetting)?.let { it.request(action.value.coerceIn(it.range)) }
        }
    }
}

/** 页面中固定不变的部分，在游戏线程构造 */
class MachineLayout(menu: BDBaseMenu, device: ItemLike, val title: String) {
    val icon: ItemIcon = ItemIcon.snapshot(ItemStack(device))
    val playerSlots = (menu.inventoryStartIndex until menu.inventoryEndIndex).toList()
    val flagSlots = menu.slots.filter { it is AbstractStackTypedSlot && it.isFake }.map { it.index }
    val resourceSlots = menu.slots.filter { it is AbstractStackTypedSlot && !it.isFake }.map { it.index }
    val text = MachineText()
}

class MachineText {
    val brand = "BEYOND DIMENSIONS"
    val syncing = tr("ui.beyonddimensions.status.syncing")
    val settings = tr("ui.beyonddimensions.machine.settings")
    val inventory = tr("ui.beyonddimensions.inventory")
    val filters = tr("ui.beyonddimensions.machine.filter_slots")
}

/**
 * 机器页面：左侧是机器内容与玩家背包，右侧是设置。
 * 子类只提供左上方的机器内容 [Machine]。
 */
abstract class MachineScreen<M : BDBaseMenu>(
    menu: M,
    title: Component,
    private val controller: MachineController,
    protected val layout: MachineLayout,
) : BdInventoryScreen<M, MachineState, MachineAction>(menu, title) {
    final override fun snapshot() = controller.snapshot()

    final override fun handle(action: MachineAction) = controller.handle(action)

    /** 左上方的机器内容 */
    @Composable protected abstract fun ColumnScope.Machine(state: MachineState, slots: ComposeMenuSlots<M>)

    @Composable
    final override fun Page(state: MachineState, slots: ComposeMenuSlots<M>) =
        MachineView(state, ::send, controller.hasSettings, slots, layout) { current, shown -> Machine(current, shown) }
}

private const val PAD = 8
private const val GAP = 10
private const val LEFT = 9 * 18 + 1
private const val RIGHT = 140

@Composable
private fun <M : BDBaseMenu> MachineView(
    state: MachineState,
    send: (MachineAction) -> Unit,
    hasSettings: Boolean,
    slots: ComposeMenuSlots<M>,
    layout: MachineLayout,
    content: @Composable ColumnScope.(MachineState, ComposeMenuSlots<M>) -> Unit,
) {
    val colors = Bd.colors
    val screen = LocalBdScreen.current
    Box(Modifier.fillMaxSize().background(OreTheme.colors.backdrop), contentAlignment = Alignment.Center) {
        val width = PAD + LEFT + (if (hasSettings) GAP + RIGHT else 0) + PAD + 2
        BdWindow(Modifier.width(width.dp).then(slots.areaModifier())) {
            BdHeader(
                layout.icon,
                layout.text.brand,
                layout.title,
                status = if (state.ready) null else BdStatus(layout.text.syncing, BdTone.Warning),
                onClose = screen::close,
            )
            Row(Modifier.padding(start = PAD.dp, end = PAD.dp, top = 7.dp, bottom = PAD.dp)) {
                Column(Modifier.width(LEFT.dp)) {
                    content(state, slots)
                    BdSectionLabel(layout.text.inventory)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.background(colors.line).padding(0.5.dp)) { BdPlayerInventory(slots, layout.playerSlots) }
                }
                if (hasSettings) {
                    Spacer(Modifier.width(GAP.dp))
                    Column(Modifier.width(RIGHT.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BdSectionLabel(layout.text.settings)
                        state.settings.forEachIndexed { index, setting -> SettingItem(index, setting, send) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(index: Int, setting: SettingView, send: (MachineAction) -> Unit) {
    when (setting) {
        is ToggleView ->
            BdSettingRow(setting.title, setting.description) {
                BdToggle(setting.checked, { send(MachineAction.Toggle(index, it)) }, setting.enabled)
            }
        is ModeView ->
            if (!setting.cycle) {
                Column {
                    BdModeSetting(setting.title, setting.options, setting.selected, setting.enabled) { send(MachineAction.Select(index, it)) }
                    if (setting.description != null) Description(setting.description)
                }
            } else {
                Column {
                    OreText(setting.title, color = Bd.colors.text, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    BdCycler(setting.options, setting.selected, setting.enabled) { send(MachineAction.Select(index, it)) }
                    if (setting.description != null) Description(setting.description)
                }
            }
        is NumberView ->
            BdSettingRow(setting.title, setting.description) {
                BdNumberField(setting.value, setting.range, setting.enabled) { send(MachineAction.Number(index, it)) }
            }
    }
}

@Composable
private fun Description(text: String) {
    Spacer(Modifier.height(2.dp))
    OreText(text, color = Bd.colors.faint, style = Bd.caption, maxLines = 2)
}

/** 选项较多时的切换器：左右箭头依次切换 */
@Composable
fun BdCycler(options: List<String>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val colors = Bd.colors
    Row(
        Modifier.fillMaxWidth().height(15.dp).background(colors.sunken, Bd.ChipShape).border(1.dp, colors.line, Bd.ChipShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BdGlyphButton(
            OreGlyph.ChevronLeft,
            null,
            { onSelect((selected - 1).mod(options.size)) },
            enabled = enabled,
            size = 13.dp,
            glyphSize = 6.dp,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            OreText(options.getOrElse(selected) { "" }, color = if (enabled) colors.accentDeep else colors.faint, maxLines = 1)
        }
        BdGlyphButton(
            OreGlyph.ChevronRight,
            null,
            { onSelect((selected + 1).mod(options.size)) },
            enabled = enabled,
            size = 13.dp,
            glyphSize = 6.dp,
        )
    }
}

/** 整数输入框：只接受数字，超出范围时收回到边界 */
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
                Modifier.fillMaxSize()
                    .background(colors.surface, Bd.ChipShape)
                    .border(1.dp, colors.line, Bd.ChipShape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                inner()
            }
        },
    )
}

/** 标记槽区：带标题的 9 列标记槽 */
@Composable
fun <M : BDBaseMenu> FlagSlots(title: String, ids: List<Int>, slots: ComposeMenuSlots<M>) {
    BdSectionLabel(title)
    Spacer(Modifier.height(4.dp))
    Box(Modifier.background(Bd.colors.line).padding(0.5.dp)) { BdSlotGrid(slots, ids, 9) }
    Spacer(Modifier.height(7.dp))
}
