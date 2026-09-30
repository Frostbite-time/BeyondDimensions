package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdThemes
import com.wintercogs.beyonddimensions.client.ui.base.at
import com.wintercogs.beyonddimensions.client.ui.base.slotAt
import com.wintercogs.beyonddimensions.client.ui.base.outsideArea
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.button.OreIconButton
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.input.OreTextField
import dev.compixel.ui.ore.layout.OreSurface
import dev.compixel.ui.ore.scroll.OreScrollbar
import dev.compixel.ui.ore.theme.OreTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW

/** 界面上不随状态变化的文字与槽位编号，在游戏线程构造 */
class StorageLabels(val title: String, val inventory: String, val search: String, val storage: List<Int>, val player: List<Int>)

/**
 * 存储终端，布局与旧版一致：左侧按钮栏，顶部标题与搜索框，可增减行数的存储网格与滚动条，下方是玩家背包。
 * [extra] 在网格与背包之间放置附加区域（合成区），高度为 [extraHeight]。
 */
open class StorageScreen<M : DimensionsNetMenu>
protected constructor(
    menu: M,
    title: Component,
    private val controller: StorageController<M>,
    labels: StorageLabels,
    extraHeight: Int,
    extra: @Composable (ComposeMenuSlots<M>, StorageState, (StorageAction) -> Unit) -> Unit,
) : BdInventoryScreen<M>(menu, title, controller, { slots -> StorageView(slots, controller.ui, labels, extraHeight, extra) }) {
    constructor(
        menu: M,
        inventory: Inventory,
        title: Component,
    ) : this(menu, title, StorageController(menu, 0), storageLabels(menu, inventory, title), 0, { _, _, _ -> })

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) menu.hasShiftDown = true
        if (!hasTextInputFocus) {
            if (hasShiftDown() && keyCode == GLFW.GLFW_KEY_Z) {
                toggleRecipeViewerSync()
                return true
            }
            val key = com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode)
            val openKey = com.wintercogs.beyonddimensions.client.init.BDShortKeys.OPEN_GUI_KEY.key
            if (Minecraft.getInstance().options.keyInventory.isActiveAndMatches(key) || openKey == key) {
                onClose()
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val result = super.keyReleased(keyCode, scanCode, modifiers)
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            menu.markForceAllUpdateClientView()
            menu.updateViewerStorage(false)
            menu.hasShiftDown = false
        }
        return result
    }

    private fun toggleRecipeViewerSync() {
        val enabled = !com.wintercogs.beyonddimensions.config.CommonConfigRuntime.searchTextWithJEIEMI
        com.wintercogs.beyonddimensions.config.CommonConfigRuntime.searchTextWithJEIEMI = enabled
        com.wintercogs.beyonddimensions.Config.INSTANCE.commonConfig.SEARCH_TEXT_WITH_JEI_EMI.set(enabled)
        com.wintercogs.beyonddimensions.Config.INSTANCE.commonConfig.SEARCH_TEXT_WITH_JEI_EMI.save()
    }

    companion object {
        fun storageLabels(menu: DimensionsNetMenu, inventory: Inventory, title: Component) =
            StorageLabels(
                title.string,
                inventory.displayName.string,
                tr("wintercogs.beyonddimensions.dimensionsguisearch"),
                menu.slots.filter { it is DisorderedStackTypedSlot }.map { it.index },
                (menu.inventoryStartIndex until menu.inventoryEndIndex).toList(),
            )
    }
}

/** 旧版的尺寸：存储部分宽 194（右侧多出滚动条一列），其下的背包部分宽 176；存储网格从 (8, 25) 开始，滚动条在 x=174 */
private const val WIDTH = 194
private const val INVENTORY_WIDTH = 176
internal const val ROW = 18

/** 旧版存储部分的高度：顶部 24、存储各行、底边 8；附加区域与背包在其下方 */
fun storageUpperHeight(lines: Int) = 24 + lines * ROW + 8

/** 旧版面板高度：存储部分、附加区域与背包的 89。控制器按同一公式推算窗口能放下的行数 */
fun storagePanelHeight(lines: Int, extraHeight: Int) = storageUpperHeight(lines) + extraHeight + 89

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun <M : DimensionsNetMenu> StorageView(
    slots: ComposeMenuSlots<M>,
    binding: UiBinding<StorageState, StorageAction>,
    labels: StorageLabels,
    extraHeight: Int,
    extra: @Composable (ComposeMenuSlots<M>, StorageState, (StorageAction) -> Unit) -> Unit,
) {
    val state = binding.value
    val send: (StorageAction) -> Unit = { binding.send(it) }
    val rowPx = with(LocalDensity.current) { ROW.dp.toPx() }
    val current by rememberUpdatedState(state)
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // 滚动条以普通滚动状态表示存储的“第几行”：游戏侧改动行号时移动滚动条，拖动滚动条时通知游戏侧
    LaunchedEffect(state.line, state.maxLine, state.lines) {
        val target = (state.line * rowPx).roundToInt()
        if (abs(scroll.value - target) >= rowPx / 2) scroll.scrollTo(target)
    }
    LaunchedEffect(scroll) {
        snapshotFlow { (scroll.value / rowPx).roundToInt() }.collect { line -> if (line != current.line) send(StorageAction.ScrollTo(line)) }
    }
    val upper = storageUpperHeight(state.lines)
    val height = storagePanelHeight(state.lines, extraHeight)
    val colors = OreTheme.colors
    Box(
        Modifier.fillMaxSize().background(colors.backdrop).onPointerEvent(PointerEventType.Scroll) { event ->
            // 与旧版相同：未被其他控件使用的滚轮在整个界面上翻动存储
            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
            if (delta != 0f) scope.launch { scroll.scrollBy(if (delta > 0) rowPx else -rowPx) }
        },
        contentAlignment = Alignment.Center,
    ) {
        // 左侧留出按钮栏的 18，使 176 宽的背包部分与旧版一样居中
        Box(Modifier.size((18 + WIDTH).dp, height.dp)) {
            Box(Modifier.offset(x = 18.dp).size(WIDTH.dp, height.dp).then(slots.areaModifier())) {
                // 旧版的面板在存储部分下方收窄：窄的背包部分不画上边，并盖住宽的存储部分的底边，只在右侧留出一截
                OreSurface(Modifier.size(WIDTH.dp, upper.dp), bottomLedge = 2.dp, ledgeColor = colors.ledge, frameEdge = colors.frameEdge) {}
                OreSurface(
                    Modifier.offset(y = (upper - 4).dp).size(INVENTORY_WIDTH.dp, (height - upper + 4).dp),
                    bottomLedge = 2.dp,
                    ledgeColor = colors.ledge,
                    frameEdge = colors.frameEdge,
                    drawTopEdge = false,
                ) {}
                // 内容按旧版界面坐标定位；面板内容从边框内侧 (2, 2) 开始
                Box(Modifier.fillMaxSize().padding(start = 2.dp, top = 2.dp)) {
                    Header(labels, state, send)
                    Box(Modifier.slotAt(8, 25)) { OreTheme(id = BdThemes.Grid) { SlotGrid(slots, labels.storage.take(state.lines * 9)) } }
                    Box(Modifier.at(174, 25).size(12.dp, (state.lines * ROW - 2).dp)) {
                        // 不可见的内容高度等于全部结果的行数，滚动条据此计算位置与长度
                        Box(Modifier.width(0.dp).fillMaxHeight().verticalScroll(scroll)) {
                            Spacer(Modifier.height(((state.maxLine + state.lines) * ROW).dp))
                        }
                        OreTheme(id = BdThemes.Scroller) { OreScrollbar(scroll, Modifier.fillMaxSize()) }
                    }
                    extra(slots, state, send)
                    OreText(labels.inventory, Modifier.at(8, upper + extraHeight - 3), maxLines = 1)
                    Box(Modifier.slotAt(8, upper + extraHeight + 7)) { PlayerInventory(slots, labels.player) }
                }
            }
            SideBar(state, send, Modifier.offset(y = 6.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Header(labels: StorageLabels, state: StorageState, send: (StorageAction) -> Unit) {
    var text by remember { mutableStateOf(state.search) }
    LaunchedEffect(state.searchRevision) { text = state.search }
    OreText(labels.title, Modifier.at(8, 8), maxLines = 1)
    // 旧版搜索框占 (60, 7) 起的 120×14；Ore 输入框高 21，放在网格上方与它居中
    OreTheme(id = BdThemes.Field) {
        Box(
            Modifier.at(60, 3).width(120.dp).onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) { event ->
                // 右键清空搜索框，与旧版相同
                if (event.buttons.isSecondaryPressed) {
                    text = ""
                    send(StorageAction.Search(""))
                }
            }
        ) {
            OreTextField(
                text,
                { value ->
                    text = value
                    send(StorageAction.Search(value))
                },
                placeholder = labels.search,
            )
        }
    }
}

/** 旧版的左侧按钮栏：16×16 的按钮，每个相隔 2，没有背景 */
@Composable
private fun SideBar(state: StorageState, send: (StorageAction) -> Unit, modifier: Modifier) {
    OreTheme(id = BdThemes.Sidebar) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for ((action, button) in state.buttons)
                // 图标四周本身留有透明边，按旧版的大小盖住整个按钮
                OreIconButton(button.tooltip, { send(action) }, Modifier.size(16.dp).then(outsideArea())) {
                    MinecraftItemIcon(button.icon, Modifier.requiredSize(16.dp))
                }
        }
    }
}

@Composable
internal fun SlotGrid(slots: ComposeMenuSlots<*>, ids: List<Int>, columns: Int = 9) {
    Column {
        for (row in ids.chunked(columns)) Row { for (id in row) slots.Slot(id) }
    }
}

/** 玩家背包三行，与快捷栏之间留 4 像素 */
@Composable
fun PlayerInventory(slots: ComposeMenuSlots<*>, ids: List<Int>, gap: Dp = 4.dp) {
    OreTheme(id = BdThemes.Grid) {
        Column {
            SlotGrid(slots, ids.take(27))
            Spacer(Modifier.height(gap))
            SlotGrid(slots, ids.drop(27))
        }
    }
}
