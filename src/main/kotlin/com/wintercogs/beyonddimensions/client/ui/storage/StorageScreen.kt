package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.init.BDShortKeys
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.common.menu.widget.StorageSort
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.inventory.OreSlot
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

/** 界面不会变化的部分：槽位编号、图标、工作台与文字，在游戏线程构造 */
class StorageLayout(menu: DimensionsNetMenu, workbenches: StorageWorkbenchHost) {
    val storageStart = menu.storageStartIndex
    val playerSlots = (menu.inventoryStartIndex until menu.inventoryEndIndex).toList()
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_TERMINAL_BLOCK.get()))
    val showSwitcher = !ClientConfigRuntime.disableMultiNetworkSwitching

    /** 下拉框里的工作台，按注册顺序 */
    val workbenches = workbenches.info
    val text = StorageText()
}

class StorageText {
    val search = tr("ui.beyonddimensions.storage.search")
    val searchTitle = tr("ui.beyonddimensions.storage.search.title")
    val searchHelp = tr("ui.beyonddimensions.storage.search.help")

    /** 搜索写法：每项一个示例与它的含义 */
    val searchSyntax =
        listOf(
            "name",
            "mod",
            "tag",
            "tooltip",
            "id",
            "type",
            "durability",
            "components",
            "exclude",
            "any",
            "escape"
        ).map {
            tr("ui.beyonddimensions.storage.search.help.$it") to tr("ui.beyonddimensions.storage.search.help.$it.hint")
        }
    val searchRule = tr("ui.beyonddimensions.storage.search.help.rule")
    val addCategory = tr("ui.beyonddimensions.storage.category.add")
    val categoryList = tr("ui.beyonddimensions.storage.category.list")
    val edit = tr("ui.beyonddimensions.storage.category.edit")
    val delete = tr("ui.beyonddimensions.storage.category.delete")
    val moveLeft = tr("ui.beyonddimensions.storage.category.move_left")
    val moveRight = tr("ui.beyonddimensions.storage.category.move_right")
    val moveUp = tr("ui.beyonddimensions.storage.category.move_up")
    val moveDown = tr("ui.beyonddimensions.storage.category.move_down")
    val editorTitle = tr("ui.beyonddimensions.storage.category.editor")
    val name = tr("ui.beyonddimensions.storage.category.name")
    val iconHint = tr("ui.beyonddimensions.storage.category.icon.hint")
    val rule = tr("ui.beyonddimensions.storage.category.rule")
    val rulePlaceholder = tr("ui.beyonddimensions.storage.category.rule.placeholder")
    val ruleHelp = tr("ui.beyonddimensions.storage.category.rule.help")
    val matches = tr("ui.beyonddimensions.storage.category.matches")
    val keys = tr("ui.beyonddimensions.storage.category.keys")
    val keysHint = tr("ui.beyonddimensions.storage.category.keys.hint")
    val syntax = tr("ui.beyonddimensions.storage.category.syntax")
    val syntaxHint = tr("ui.beyonddimensions.storage.category.syntax.hint")
    val takeOne = tr("ui.beyonddimensions.storage.slot_menu.take_one")
    val take = tr("ui.beyonddimensions.storage.slot_menu.take")
    val takeUnit = tr("ui.beyonddimensions.storage.slot_menu.take.unit")
    val addTo = tr("ui.beyonddimensions.storage.slot_menu.add_to")
    val storage = tr("ui.beyonddimensions.storage.storage")
    val workbench = tr("ui.beyonddimensions.storage.workbench")
    val switcher = tr("ui.beyonddimensions.storage.switch_network")
    val view = tr("ui.beyonddimensions.storage.view")
    val sort = tr("ui.beyonddimensions.storage.sort")
    val secondarySort = tr("ui.beyonddimensions.storage.secondary_sort")
    val none = tr("ui.beyonddimensions.storage.sort.none")
    val reverse = tr("ui.beyonddimensions.storage.reverse")
    val layout = tr("ui.beyonddimensions.storage.layout")
    val columns = tr("ui.beyonddimensions.storage.columns")
    val rows = tr("ui.beyonddimensions.storage.rows")
    val interaction = tr("ui.beyonddimensions.storage.interaction")
    val rightClickMenu = tr("ui.beyonddimensions.storage.right_click_menu")
    val rightClickMenuHint = tr("ui.beyonddimensions.storage.right_click_menu.hint")
    val keepSearch = tr("ui.beyonddimensions.storage.keep_search")
    val syncSearch = tr("ui.beyonddimensions.storage.sync_search")
    val syncSearchHint = tr("ui.beyonddimensions.storage.sync_search.hint")
    val inventory = tr("ui.beyonddimensions.inventory")
    val keepCrafting = tr("ui.beyonddimensions.crafting.keep")
    val keepCraftingHint = tr("ui.beyonddimensions.crafting.keep.hint")
    val showWorkbench = tr("ui.beyonddimensions.storage.workbench.show")
    val hideWorkbench = tr("ui.beyonddimensions.storage.workbench.hide")
    val chooseWorkbench = tr("ui.beyonddimensions.storage.workbench.choose")
    val returnStorage = tr("ui.beyonddimensions.storage.workbench.return.storage")
    val returnInventory = tr("ui.beyonddimensions.storage.workbench.return.inventory")
    val returnSwitch = tr("ui.beyonddimensions.storage.workbench.return.switch")
    val sortNames = StorageSort.entries.associateWith { tr("ui.beyonddimensions.storage.sort.${it.name.lowercase()}") }
}

/** 存储终端与合成终端 */
class StorageScreen(private val storageMenu: DimensionsNetMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<DimensionsNetMenu, StorageState, StorageAction>(storageMenu, title) {
    private val workbenches = StorageWorkbenchHost(storageMenu, this, this.inventory, ::requestClose)
    private val controller = StorageController(storageMenu, workbenches)
    private val layout = StorageLayout(storageMenu, workbenches)

    init {
        adapter.resourceMenu = controller::openSlotMenu
    }

    // 打开界面与窗口缩放时调用：先按屏幕定好格子行列，快照里就是最终尺寸
    override fun init() {
        controller.fitViewport()
        super.init()
    }

    override fun snapshot() = controller.snapshot()

    override fun handle(action: StorageAction) = controller.handle(action)

    @Composable
    override fun Content(state: StorageState, slots: ComposeMenuSlots<DimensionsNetMenu>) =
        StorageView(state, ::send, ::requestClose, slots, layout, pages, workbenches)

    override fun inventoryTick() {
        super.inventoryTick()
        controller.tick()
        workbenches.update()
    }

    // 工作台送出的动作随即处理，不等下一刻
    override fun afterInput() {
        super.afterInput()
        workbenches.update()
    }

    override fun menuClosed() {
        super.menuClosed()
        workbenches.close()
        controller.saveSearch()
        controller.saveNow()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) storageMenu.hasShiftDown = true
        if (!hasTextInputFocus && BDShortKeys.OPEN_GUI_KEY.matches(keyCode, scanCode)) {
            onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val handled = super.keyReleased(keyCode, scanCode, modifiers)
        // 按住 Shift 期间只更新数量，松开后再整理视图，避免连续 Shift 点击时格子跳动
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            storageMenu.hasShiftDown = false
            storageMenu.markForceAllUpdateClientView()
            storageMenu.updateViewerStorage(false)
        }
        return handled
    }
}

/** 左右相邻的分区之间的间距 */
private const val GAP = 7

/** 存储格、工作区与背包区上下相邻时的间距 */
private const val SECTION_GAP = 3
private const val SCREEN_MARGIN = 8

/** 格子区域（含边框）、间隔与滚动条的总宽度 */
private fun contentWidth(columns: Int) = columns * SLOT_PITCH + 2 + 2 + SCROLLBAR_WIDTH

/** 窗口中除格子区域外的宽度 */
private const val CHROME_WIDTH = SIDE_RAIL_WIDTH + PAGE_PADDING_X * 2 + 2

/** 分类编辑区：与主内容的间隔、分隔线、间隔与七列标记格 */
private const val EDITOR_COLUMNS = 7
private const val EDITOR_CONTENT_WIDTH = EDITOR_COLUMNS * SLOT_PITCH + 1
private const val EDITOR_WIDTH = GAP + 1 + GAP + EDITOR_CONTENT_WIDTH
private const val EDITOR_MILLIS = 180

/** 除格子行与下方各区外的固定高度：标题栏、页面留白、搜索栏、分类标签栏与格子边框 */
private const val CHROME_HEIGHT = HEADER_HEIGHT + PAGE_PADDING_TOP + FIELD_HEIGHT + 3 + 13 + 3 + 2 + PAGE_PADDING_BOTTOM

/** 工作区下拉框一行与背包标题的高度：与行末 11 dp 的小按钮一样高 */
private const val LABEL_HEIGHT = 11

/** 工作台内容的顶边：下拉框一行与其下的间距。背包标题连同其下的间距也是这么高 */
private const val WORKBENCH_CONTENT_TOP = LABEL_HEIGHT + 3

/** 背包区：背包三行、间隔与快捷栏，外框每边占半个像素 */
private const val INVENTORY_WIDTH = 9 * SLOT_PITCH + 1
private const val INVENTORY_HEIGHT = 3 * SLOT_PITCH + 3 + SLOT_PITCH + 1

/** 工作区并排在背包区左侧时，两者之间的间距、分隔线与间距 */
private const val SPLIT_WIDTH = GAP + 1 + GAP

/** 工作区展开与收起的时长，格子的行数同时以同样的时长增减 */
private const val WORKBENCH_MILLIS = 180

/** 下拉列表的一行，最多同时列出几行，再多就在列表里滚动 */
private const val WORKBENCH_ROW_HEIGHT = 14
private const val WORKBENCH_LIST_ROWS = 4

/** 工作区的高度：下拉框一行、间距与工作台内容 */
private fun workbenchAreaHeight(workbenches: StorageWorkbenchHost) = WORKBENCH_CONTENT_TOP + workbenches.contentHeight

/** 有工作台且内容区有 [width] 宽时工作区与背包区并排，否则工作区在背包区上方 */
private fun workbenchBeside(width: Int, workbenches: StorageWorkbenchHost) =
    !workbenches.isEmpty && width >= workbenches.contentWidth + SPLIT_WIDTH + INVENTORY_WIDTH

/**
 * 存储格下方各区连同上方间距的高度：背包区，[shown] 时还有工作区。
 * 并排或收起工作区时背包区上方有标题，与下拉框一行连同间距一样高。并排时槽位与工作台内容对齐，收起工作区时位置不变；
 * 工作区不比这更高（见 [com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbenches.MAX_HEIGHT]），不占格子的行数
 */
internal fun bottomHeight(width: Int, workbenches: StorageWorkbenchHost, shown: Boolean): Int {
    val areas = when {
        workbenchBeside(width, workbenches) ->
            maxOf(workbenchAreaHeight(workbenches), WORKBENCH_CONTENT_TOP + INVENTORY_HEIGHT)
        !shown -> WORKBENCH_CONTENT_TOP + INVENTORY_HEIGHT
        else -> workbenchAreaHeight(workbenches) + SECTION_GAP + INVENTORY_HEIGHT
    }
    return SECTION_GAP + areas
}

/**
 * 格子的实际列数与行数：首选行列按屏幕上放得下的格数收窄。[screenWidth]、[screenHeight] 是整个屏幕的 dp 尺寸，
 * [bottomHeight] 给出格子下方各区在某个内容宽度下的高度
 */
internal fun fittedGridSize(
    screenWidth: Int,
    screenHeight: Int,
    editorOpen: Boolean,
    preferredColumns: Int,
    preferredRows: Int,
    bottomHeight: (contentWidth: Int) -> Int,
): Pair<Int, Int> {
    // 内容区居中，竖条对面也留出一条竖条宽的空位；分类编辑区打开时窗口向右扩展
    val fitColumns =
        (screenWidth - SCREEN_MARGIN * 2 - CHROME_WIDTH - SIDE_RAIL_WIDTH - contentWidth(0) -
                if (editorOpen) EDITOR_WIDTH else 0) / SLOT_PITCH
    val columns = fitColumns.coerceAtMost(preferredColumns).coerceAtLeast(9)
    val fitRows = (screenHeight - SCREEN_MARGIN * 2 - CHROME_HEIGHT - bottomHeight(contentWidth(columns))) / SLOT_PITCH
    return columns to fitRows.coerceAtMost(preferredRows).coerceAtLeast(2)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun StorageView(
    state: StorageState,
    send: (StorageAction) -> Unit,
    close: () -> Unit,
    slots: ComposeMenuSlots<DimensionsNetMenu>,
    layout: StorageLayout,
    pages: BdPageHost<DimensionsNetMenu>,
    workbenches: StorageWorkbenchHost,
) {
    val colors = Bd.colors
    val text = layout.text
    val page = rememberPageSelection()
    val settingsOpen = page.settings
    // 排序与配方选择的浮层盖在槽位上方，打开期间停用槽位
    val popovers = remember { BdPopovers() }
    slots.Interaction(enabled = popovers.open == 0)
    // Tab 只用来进出搜索框
    val searchFocus = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    CompositionLocalProvider(LocalBdPopovers provides popovers) {
        BdScreenFrame {
            val editorOpen = state.editor != null
            val gridColumns = state.columns
            val gridRows = state.rows
            val width = contentWidth(gridColumns)
            // 编辑区收起期间仍显示最后的内容，直到宽度收回
            val lastEditor = remember { arrayOfNulls<CategoryEditor>(1) }
            state.editor?.let { lastEditor[0] = it }
            val editorWidth by animateIntAsState(
                if (editorOpen) EDITOR_WIDTH else 0,
                tween(EDITOR_MILLIS, easing = FastOutSlowInEasing),
                label = "category editor",
            )
            val mainAlpha by animateFloatAsState(if (page.main) 1f else 0f, tween(150), label = "editor fade")
            BdTabbedWindow(
                slots.areaModifier()
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        // 分类标签与编辑区自己滚动时不翻动格子
                        if (event.changes.any { it.isConsumed }) return@onPointerEvent
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (delta != 0f && page.main) send(StorageAction.Scroll(if (delta > 0) 1 else -1))
                    }
                    // 焦点在窗口里时由这里处理 Tab；没有焦点时 Tab 进入窗口后改到搜索框
                    .onPreviewKeyEvent { event ->
                        if (event.key != Key.Tab) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyDown) {
                            if (searchFocused) focusManager.clearFocus() else searchFocus.requestFocus()
                        }
                        true
                    }
                    .focusProperties {
                        onEnter = {
                            if (requestedFocusDirection == FocusDirection.Next ||
                                requestedFocusDirection == FocusDirection.Previous
                            ) searchFocus.requestFocus()
                        }
                    }
                    .focusGroup(),
                width = (CHROME_WIDTH + width + editorWidth).dp,
                header = {
                    BdHeader(
                        layout.icon,
                        state.networkName.ifEmpty { text.storage },
                        close,
                        tag = if (state.networkId >= 0) "#%04d".format(state.networkId) else null,
                    )
                },
                tabs = {
                    BdRailTab(text.storage, BdIcons.Main, selected = page.main) { page.open(BdPages.MAIN) }
                    // 展开或收起主页面上的工作区；在其他页上点它时回到主页面并展开
                    if (layout.workbenches.isNotEmpty()) {
                        val hide = page.main && state.workbenchShown
                        BdRailTab(
                            if (hide) text.hideWorkbench else text.showWorkbench,
                            BdIcons.Crafting,
                            selected = false,
                            active = state.workbenchShown,
                        ) {
                            send(StorageAction.ShowWorkbench(!hide))
                            page.open(BdPages.MAIN)
                        }
                    }
                    InjectedTabs(pages, page)
                    // 切换网络不是页面，用未选中页签的样子当按钮
                    if (layout.showSwitcher) {
                        BdRailTab(text.switcher, BdIcons.Switch, selected = false) {
                            send(StorageAction.OpenSwitcher)
                        }
                    }
                },
                pinned = {
                    BdRailTab(text.view, BdIcons.Settings, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
                },
            ) {
                BdMainPage(page.main) {
                    Toolbar(state, send, text, width, searchFocus) { searchFocused = it }
                    Spacer(Modifier.height(3.dp))
                    CategoryTabs(state, send, text, width)
                    Spacer(Modifier.height(3.dp))
                    // 工作区展开与收起时格子增减行数，高度与工作区一同以动画变化
                    val gridHeight by animateIntAsState(
                        gridRows * SLOT_PITCH + 2,
                        tween(WORKBENCH_MILLIS, easing = FastOutSlowInEasing),
                        label = "storage rows",
                    )
                    Row(Modifier.height(gridHeight.dp)) {
                        // 右键菜单在按下右键的位置展开
                        var pressAt by remember { mutableStateOf(IntOffset.Zero) }
                        Box(
                            Modifier.width((gridColumns * SLOT_PITCH + 2).dp).fillMaxHeight()
                                .bdInset(colors).padding(1.dp).clipToBounds()
                                .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
                                    pressAt = it.changes.first().position.round()
                                }
                        ) {
                            val ids = List(gridColumns * gridRows) { layout.storageStart + it }
                            BdSlotGrid(slots, ids, gridColumns)
                            state.slotMenu?.let { menu ->
                                key(menu.serial) {
                                    SlotMenuPopup(
                                        menu,
                                        pressAt,
                                        send,
                                        text
                                    )
                                }
                            }
                        }
                        if (state.totalRows > gridRows) {
                            Spacer(Modifier.width(2.dp))
                            BdScrollbar(
                                state.firstRow,
                                gridRows,
                                state.totalRows,
                                { send(StorageAction.ScrollTo(it)) },
                                Modifier.width(SCROLLBAR_WIDTH.dp).fillMaxHeight(),
                            )
                        }
                    }
                    WorkbenchAndInventory(state, send, slots, layout, workbenches, width)
                    // 给编辑区留出位置
                    Spacer(Modifier.width((width + editorWidth).dp))
                }
                val editor = lastEditor[0]
                if (editorWidth > 0 && editor != null) {
                    Box(
                        Modifier.matchParentSize()
                            .padding(
                                top = PAGE_PADDING_TOP.dp,
                                bottom = PAGE_PADDING_BOTTOM.dp,
                                end = PAGE_PADDING_X.dp
                            )
                            .graphicsLayer { alpha = mainAlpha },
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        // 编辑区的内容按完整宽度排好，展开时从主内容后面露出来
                        Box(Modifier.width(editorWidth.dp).fillMaxHeight().clipToBounds()) {
                            Row(
                                Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                                    .width(EDITOR_WIDTH.dp)
                                    .fillMaxHeight()
                            ) {
                                Spacer(Modifier.width(GAP.dp))
                                Box(Modifier.width(1.dp).fillMaxHeight().background(colors[BdColors.line]))
                                Spacer(Modifier.width(GAP.dp))
                                key(editor.index) {
                                    BdScrollColumn(Modifier.width(EDITOR_CONTENT_WIDTH.dp).fillMaxHeight()) {
                                        CategoryEditorPanel(editor, send, text, enabled = editorOpen && page.main)
                                    }
                                }
                            }
                        }
                    }
                }
                InjectedPages(pages, page)
                BdTabPage(settingsOpen) { ViewOptions(state, send, text, settingsOpen) }
            }
        }
    }
}

/**
 * 存储格下方：背包区，展开时还有工作区。内容区够宽时工作区并排在背包区左侧，否则在背包区上方；
 * 工作区展开与收起时以动画伸缩
 */
@Composable
private fun WorkbenchAndInventory(
    state: StorageState,
    send: (StorageAction) -> Unit,
    slots: ComposeMenuSlots<DimensionsNetMenu>,
    layout: StorageLayout,
    workbenches: StorageWorkbenchHost,
    width: Int,
) {
    val colors = Bd.colors
    val resize = tween<IntSize>(WORKBENCH_MILLIS, easing = FastOutSlowInEasing)
    val fade = tween<Float>(WORKBENCH_MILLIS, easing = FastOutSlowInEasing)
    Spacer(Modifier.height(SECTION_GAP.dp))
    if (workbenchBeside(width, workbenches)) {
        // 两者一起居中，工作区伸出时背包区随之右移
        val height = maxOf(workbenchAreaHeight(workbenches), WORKBENCH_CONTENT_TOP + INVENTORY_HEIGHT)
        Row(Modifier.width(width.dp).height(height.dp), horizontalArrangement = Arrangement.Center) {
            AnimatedVisibility(
                state.workbenchShown,
                enter = expandHorizontally(resize, Alignment.End) + fadeIn(fade),
                exit = shrinkHorizontally(resize, Alignment.End) + fadeOut(fade),
            ) {
                Row {
                    WorkbenchArea(state, send, layout, workbenches)
                    Spacer(Modifier.width(GAP.dp))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors[BdColors.line]))
                    Spacer(Modifier.width(GAP.dp))
                }
            }
            InventoryArea(slots, layout, titled = true)
        }
    } else {
        // 工作区与背包区一样宽，一起在内容区里居中；背包的槽位在这个宽度里居中，标题靠左
        val columnWidth = maxOf(workbenches.contentWidth, INVENTORY_WIDTH)
        Box(Modifier.width(width.dp), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.width(columnWidth.dp)) {
                AnimatedVisibility(
                    state.workbenchShown,
                    enter = expandVertically(resize, Alignment.Top) + fadeIn(fade),
                    exit = shrinkVertically(resize, Alignment.Top) + fadeOut(fade),
                ) {
                    Column {
                        WorkbenchArea(state, send, layout, workbenches)
                        Spacer(Modifier.height(SECTION_GAP.dp))
                    }
                }
                InventoryArea(slots, layout, titled = !state.workbenchShown, width = columnWidth)
            }
        }
    }
}

/**
 * 背包区，[width] 宽，玩家背包的槽位居中。[titled] 时上方是靠左的标题，与工作区的下拉框一行连同间距一样高，
 * 并排时两边的槽位对齐；标题随工作区的展开与收起以动画伸缩。
 * 宽度与背包相差偶数个界面像素，居中后槽位的细框不会落在半个像素上
 */
@Composable
private fun InventoryArea(
    slots: ComposeMenuSlots<DimensionsNetMenu>,
    layout: StorageLayout,
    titled: Boolean,
    width: Int = INVENTORY_WIDTH,
) {
    val resize = tween<IntSize>(WORKBENCH_MILLIS, easing = FastOutSlowInEasing)
    val fade = tween<Float>(WORKBENCH_MILLIS, easing = FastOutSlowInEasing)
    Column(Modifier.width(width.dp)) {
        AnimatedVisibility(
            titled,
            enter = expandVertically(resize, Alignment.Top) + fadeIn(fade),
            exit = shrinkVertically(resize, Alignment.Top) + fadeOut(fade),
        ) {
            Column {
                BdSectionLabel(layout.text.inventory, Modifier.height(LABEL_HEIGHT.dp))
                Spacer(Modifier.height((WORKBENCH_CONTENT_TOP - LABEL_HEIGHT).dp))
            }
        }
        Box(Modifier.align(Alignment.CenterHorizontally).bdSlotFrame(Bd.colors)) {
            BdPlayerInventory(
                slots,
                layout.playerSlots
            )
        }
    }
}

/**
 * 工作区：左上角的下拉框选择工作台，同一行右端是这个工作台的按钮，放着物品的工作台最右还有退回方向按钮；
 * 下方是居中的内容
 */
@Composable
private fun WorkbenchArea(
    state: StorageState,
    send: (StorageAction) -> Unit,
    layout: StorageLayout,
    workbenches: StorageWorkbenchHost,
) {
    val id = state.workbench
    Column(Modifier.width(workbenches.contentWidth.dp)) {
        Row(Modifier.fillMaxWidth().height(LABEL_HEIGHT.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                WorkbenchChooser(layout.workbenches, id, layout.text) { send(StorageAction.SelectWorkbench(it)) }
            }
            workbenches.Actions(id)
            if (layout.workbenches.firstOrNull { it.id == id }?.holdsItems == true) {
                Spacer(Modifier.width(2.dp))
                ReturnToggle(state.returnToStorage, layout.text) { send(StorageAction.ReturnToStorage(it)) }
            }
        }
        Spacer(Modifier.height(3.dp))
        // 工作台比工作区小时居中，偏移取整到界面像素，槽位的细框不会落在半个像素上
        Layout(
            { Box { workbenches.Content(id) } },
            Modifier.size(workbenches.contentWidth.dp, workbenches.contentHeight.dp).clipToBounds(),
        ) { measurables, constraints ->
            val workbench = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
            val pixel = 1.dp.roundToPx().coerceAtLeast(1)
            layout(constraints.maxWidth, constraints.maxHeight) {
                workbench.place(
                    (constraints.maxWidth - workbench.width) / 2 / pixel * pixel,
                    (constraints.maxHeight - workbench.height) / 2 / pixel * pixel,
                )
            }
        }
    }
}

/**
 * 工作台被清空时（关闭界面且不保留、填入配方）物品优先退回的地方，所有工作台共用：
 * 向上的箭头是存储，向下的是背包，点一下换成另一个
 */
@Composable
private fun ReturnToggle(toStorage: Boolean, text: StorageText, onChange: (Boolean) -> Unit) {
    val colors = Bd.colors
    BdTooltip({
        OreText(
            if (toStorage) text.returnStorage else text.returnInventory,
            color = colors[BdColors.text],
            style = Bd.caption
        )
        OreText(text.returnSwitch, color = colors[BdColors.faint], style = Bd.caption)
    }) {
        BdGlyphButton(
            if (toStorage) BdIcons.ArrowUp else BdIcons.ArrowDown,
            null,
            { onChange(!toStorage) },
            size = 11.dp
        )
    }
}

/** 选择工作台的下拉框：当前工作台的名称，不带图标，与背包区的标题一样不显眼；点开后列出全部工作台，多到放不下时在列表里滚动 */
@Composable
private fun WorkbenchChooser(
    workbenches: List<WorkbenchInfo>,
    selected: String,
    text: StorageText,
    onSelect: (String) -> Unit
) {
    val colors = Bd.colors
    val current = workbenches.firstOrNull { it.id == selected } ?: return
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // 只有一个工作台时没有可选的，只显示它的名称
    val enabled = workbenches.size > 1
    val lit = enabled && (hovered || open)
    val tint = if (lit) colors[BdColors.accentDeep] else colors[BdColors.muted]
    Box {
        BdTooltip(text.chooseWorkbench, enabled = enabled && !open) {
            Row(
                Modifier.height(LABEL_HEIGHT.dp)
                    .hoverable(interaction, enabled)
                    .bdClickable(interaction, enabled = enabled) { open = !open }
                    .background(if (lit) colors[BdColors.accentSoft] else Color.Transparent)
                    .padding(horizontal = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OreText(
                    current.title,
                    Modifier.weight(1f, fill = false),
                    color = tint,
                    style = Bd.overline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (enabled) {
                    Spacer(Modifier.width(3.dp))
                    BdIcon(BdIcons.ChevronDown, color = tint)
                }
            }
        }
        if (open) {
            BdPopover({ open = false }, alignStart = true) {
                BdScrollColumn(
                    Modifier.heightIn(max = (WORKBENCH_LIST_ROWS * WORKBENCH_ROW_HEIGHT).dp),
                    wheelStep = WORKBENCH_ROW_HEIGHT.dp
                ) {
                    for (workbench in workbenches) {
                        WorkbenchOption(workbench, workbench.id == selected) {
                            onSelect(workbench.id)
                            open = false
                        }
                    }
                }
            }
        }
    }
}

/** 下拉列表里的一个工作台：选中的一项前有强调色方点 */
@Composable
private fun WorkbenchOption(workbench: WorkbenchInfo, selected: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .height(WORKBENCH_ROW_HEIGHT.dp)
            .hoverable(interaction)
            .bdClickable(interaction, enabled = !selected, onClick = onClick)
            .background(if (hovered && !selected) colors[BdColors.accentSoft] else Color.Transparent)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(3.dp).background(if (selected) colors[BdColors.accent] else Color.Transparent))
        Spacer(Modifier.width(4.dp))
        OreText(
            workbench.title,
            color = if (selected) colors[BdColors.accentDeep] else colors[BdColors.text],
            maxLines = 1
        )
    }
}

@Composable
private fun Toolbar(
    state: StorageState,
    send: (StorageAction) -> Unit,
    text: StorageText,
    width: Int,
    searchFocus: FocusRequester,
    onSearchFocus: (Boolean) -> Unit,
) {
    // 配方查看器改动搜索时同步进来
    val search = rememberLocalText(state.search)
    val colors = Bd.colors
    Row(Modifier.width(width.dp), verticalAlignment = Alignment.CenterVertically) {
        BdSearchField(
            search.value,
            { search.edit(it) { value -> send(StorageAction.Search(value)) } },
            text.search,
            Modifier.weight(1f).focusRequester(searchFocus).onFocusChanged { onSearchFocus(it.hasFocus) },
            help = { SearchHelp(text) },
        )
        Spacer(Modifier.width(4.dp))
        var open by remember { mutableStateOf(false) }
        Box {
            BdChip({ open = !open }, active = open) {
                BdIcon(BdIcons.Bars, color = colors[BdColors.faint])
                Spacer(Modifier.width(3.dp))
                OreText(text.sortNames[state.sort] ?: text.sort, color = colors[BdColors.text], maxLines = 1)
                Spacer(Modifier.width(3.dp))
                BdIcon(
                    if (state.reverse) BdIcons.ArrowUp else BdIcons.ArrowDown,
                    color = colors[BdColors.accent]
                )
            }
            if (open) BdPopover({ open = false }) { SortOptions(state, send, text) }
        }
    }
}

/** 搜索框的悬停说明：每行一个写法的示例与含义，最后是组合规则 */
@Composable
private fun SearchHelp(text: StorageText, rule: String = text.searchRule) {
    val colors = Bd.colors
    OreText(text.searchHelp, color = colors[BdColors.text], style = Bd.caption, maxLines = 1)
    // 示例一列等宽；含义较长时在自己的一列里换行，不被截断
    val measurer = rememberTextMeasurer()
    val style = Bd.caption
    val examples = with(LocalDensity.current) {
        text.searchSyntax.maxOf { (example, _) -> measurer.measure(example, style, maxLines = 1).size.width }.toDp()
    }
    Column {
        for ((example, meaning) in text.searchSyntax) {
            Row {
                OreText(
                    example,
                    Modifier.width(examples),
                    color = colors[BdColors.accentDeep],
                    style = Bd.caption,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                OreText(meaning, color = colors[BdColors.muted], style = Bd.caption)
            }
        }
    }
    OreText(rule, color = colors[BdColors.faint], style = Bd.caption)
}

/**
 * 输入框的本地文字，不必等一刻的往返。快照带回的较早的文字不算外部改动；
 * 外部改动（如配方查看器的搜索、点选的写法）才同步进来
 */
private class LocalText(initial: String) {
    var value by mutableStateOf(initial)
        private set
    private val pending = ArrayDeque<String>()

    fun edit(text: String, send: (String) -> Unit) {
        value = text
        pending.addLast(text)
        send(text)
    }

    fun receive(external: String) {
        val echo = pending.indexOf(external)
        if (echo >= 0) repeat(echo + 1) { pending.removeFirst() } else value = external
    }
}

@Composable
private fun rememberLocalText(external: String): LocalText {
    val text = remember { LocalText(external) }
    LaunchedEffect(external) { text.receive(external) }
    return text
}

/**
 * 分类标签：标签后是新建按钮，最右侧的下拉列出全部标签。
 * 放不下时用滚轮横向滚动；右键打开编辑、移动与删除的菜单，也可以拖动排序
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CategoryTabs(state: StorageState, send: (StorageAction) -> Unit, text: StorageText, width: Int) {
    val colors = Bd.colors
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 24.dp.toPx() }
    // 拖动中的标签与它的位移；各标签的横向位置用来算放下后的次序
    var dragged by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val spans = remember { HashMap<Int, Pair<Float, Float>>() }
    var viewport by remember { mutableIntStateOf(0) }
    val count = state.categories.size
    val editing = state.editor?.index ?: -1

    fun dropIndex(): Int {
        val (start, size) = spans[dragged] ?: return dragged
        val center = start + dragOffset + size / 2
        return (0 until count).count { it != dragged && spans[it]?.let { (s, w) -> s + w / 2 < center } == true }
    }

    // 新加的标签要等一帧排好位置，然后直接跳到位
    suspend fun reveal(index: Int) {
        withFrameNanos {}
        val (start, size) = spans[index] ?: return
        val target =
            when {
                start < scroll.value -> start
                start + size > scroll.value + viewport -> start + size - viewport
                else -> return
            }
        scroll.scrollTo(target.roundToInt())
    }
    LaunchedEffect(state.category) { if (state.category >= 0) reveal(state.category) }
    LaunchedEffect(editing) { if (editing >= 0) reveal(editing) }

    Row(
        Modifier.width(width.dp).height(13.dp).drawBehind {
            drawLine(
                colors[BdColors.line],
                Offset(0f, size.height - 0.5f),
                Offset(size.width, size.height - 0.5f),
                1.dp.toPx()
            )
        },
        verticalAlignment = Alignment.Top,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            Row(
                Modifier.weight(1f, fill = false)
                    .fillMaxHeight()
                    .onSizeChanged { viewport = it.width }
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val delta = event.changes.sumOf { (it.scrollDelta.y + it.scrollDelta.x).toDouble() }
                        if (scroll.maxValue > 0 && delta != 0.0) {
                            scope.launch { scroll.scrollBy(if (delta > 0) step else -step) }
                            event.changes.forEach { it.consume() }
                        }
                    }
                    .horizontalScroll(scroll, enabled = false),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                state.categories.forEachIndexed { index, tab ->
                    CategoryTabView(
                        tab,
                        index,
                        count,
                        selected = index == state.category,
                        editing = index == editing,
                        send = send,
                        text = text,
                        modifier = Modifier
                            .onGloballyPositioned {
                                spans[index] = it.positionInParent().x to it.size.width.toFloat()
                            }
                            .then(
                                if (index == dragged) Modifier.zIndex(1f).graphicsLayer { translationX = dragOffset }
                                else Modifier
                            )
                            .pointerInput(index, count) {
                                detectDragGestures(
                                    onDragStart = {
                                        // 按下后稍一移动就算拖动；拖动同样打开这个分类，点击不会落空
                                        send(StorageAction.SelectCategory(index))
                                        dragged = index
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        val target = dropIndex()
                                        if (target != dragged) send(StorageAction.MoveCategory(dragged, target))
                                        dragged = -1
                                    },
                                    onDragCancel = { dragged = -1 },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset += amount.x
                                }
                            },
                    )
                }
            }
            Spacer(Modifier.width(if (count > 0) 6.dp else 0.dp))
            BdGlyphButton(
                BdIcons.Plus,
                text.addCategory,
                { send(StorageAction.AddCategory) },
                size = 11.dp,
            )
        }
        Spacer(Modifier.width(4.dp))
        CategoryList(state, editing, send, text)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CategoryTabView(
    tab: CategoryTab,
    index: Int,
    count: Int,
    selected: Boolean,
    editing: Boolean,
    send: (StorageAction) -> Unit,
    text: StorageText,
    modifier: Modifier,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuAt by remember { mutableStateOf<IntOffset?>(null) }
    Box(
        modifier
            .width(IntrinsicSize.Max)
            .fillMaxHeight()
            .hoverable(interaction)
            .bdClickable(interaction, enabled = !selected) { send(StorageAction.SelectCategory(index)) }
            .onPointerEvent(PointerEventType.Press) {
                if (it.buttons.isSecondaryPressed) menuAt = it.changes.first().position.round()
            }
    ) {
        Column(Modifier.fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tab.icon != null) {
                    MinecraftItemIcon(tab.icon, Modifier.size(8.dp))
                    Spacer(Modifier.width(2.dp))
                } else if (tab.name.isBlank()) {
                    // 没有名字也没有图标的分类画一个漏斗，不然标签宽度为零，看不见也点不到
                    BdIcon(
                        BdIcons.Funnel,
                        color = if (selected || hovered) colors[BdColors.text] else colors[BdColors.muted]
                    )
                }
                OreText(
                    tab.name,
                    color = if (selected || hovered) colors[BdColors.text] else colors[BdColors.muted],
                    maxLines = 1
                )
                if (editing) {
                    Spacer(Modifier.width(2.dp))
                    BdIcon(BdIcons.Pencil, color = colors[BdColors.accent])
                }
            }
            Spacer(Modifier.weight(1f))
            // 选中的分类下方一条两像素的发光线
            if (selected) Box(
                Modifier.fillMaxWidth().height(2.dp).drawBehind {
                    val p = 1.dp.toPx()
                    glowLine(colors, 0f, 0f, size.width, horizontal = true, p)
                    glowLine(colors, 0f, p, size.width, horizontal = true, p)
                }
            )
        }
        menuAt?.let { point ->
            CategoryMenu(point, index, count, vertical = false, send, text, onDismiss = { menuAt = null })
        }
    }
}

/** 分类的右键菜单：编辑、调整次序与删除。[vertical] 为 true 时次序说上移、下移 */
@Composable
private fun CategoryMenu(
    point: IntOffset,
    index: Int,
    count: Int,
    vertical: Boolean,
    send: (StorageAction) -> Unit,
    text: StorageText,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
) {
    BdPointMenu(point, onDismiss) {
        BdMenuRow(text.edit) {
            onDismiss()
            onEdit()
            send(StorageAction.EditCategory(index))
        }
        if (index > 0) BdMenuRow(if (vertical) text.moveUp else text.moveLeft) {
            onDismiss()
            send(StorageAction.MoveCategory(index, index - 1))
        }
        if (index < count - 1) BdMenuRow(if (vertical) text.moveDown else text.moveRight) {
            onDismiss()
            send(StorageAction.MoveCategory(index, index + 1))
        }
        BdMenuRow(text.delete) {
            onDismiss()
            send(StorageAction.DeleteCategory(index))
        }
    }
}

/** 下拉列出全部标签 */
@Composable
private fun CategoryList(state: StorageState, editing: Int, send: (StorageAction) -> Unit, text: StorageText) {
    var open by remember { mutableStateOf(false) }
    Box {
        BdGlyphButton(
            BdIcons.ChevronDown,
            text.categoryList,
            { open = !open },
            enabled = state.categories.isNotEmpty(),
            selected = open,
            size = 11.dp,
        )
        if (open) {
            BdPopover({ open = false }) {
                BdScrollColumn(Modifier.widthIn(max = 160.dp).heightIn(max = 150.dp)) {
                    state.categories.forEachIndexed { index, tab ->
                        CategoryOption(
                            tab,
                            index,
                            state.categories.size,
                            selected = index == state.category,
                            editing = index == editing,
                            send = send,
                            text = text,
                            // 打开编辑区时收起列表；移动与删除后列表留着
                            onEdit = { open = false },
                        ) {
                            send(StorageAction.SelectCategory(index))
                            open = false
                        }
                    }
                }
            }
        }
    }
}

/** 标签列表里的一行：左键打开这个分类，右键是与标签相同的菜单 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CategoryOption(
    tab: CategoryTab,
    index: Int,
    count: Int,
    selected: Boolean,
    editing: Boolean,
    send: (StorageAction) -> Unit,
    text: StorageText,
    onEdit: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuAt by remember { mutableStateOf<IntOffset?>(null) }
    // 菜单以这一行为锚点，展开在按下右键的位置
    Box {
        menuAt?.let { point ->
            CategoryMenu(point, index, count, vertical = true, send, text, { menuAt = null }, onEdit)
        }
        Row(
            Modifier.fillMaxWidth()
                .height(13.dp)
                .hoverable(interaction)
                .bdClickable(interaction, enabled = !selected, onClick = onClick)
                .onPointerEvent(PointerEventType.Press) {
                    if (it.buttons.isSecondaryPressed) menuAt = it.changes.first().position.round()
                }
                .background(if (hovered && !selected) colors[BdColors.accentSoft] else Color.Transparent)
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(3.dp).background(if (selected) colors[BdColors.accent] else Color.Transparent))
            Spacer(Modifier.width(4.dp))
            // 没有图标的分类显示一个漏斗作占位
            Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
                if (tab.icon != null) MinecraftItemIcon(tab.icon, Modifier.fillMaxSize())
                else BdIcon(BdIcons.Funnel, color = colors[BdColors.faint])
            }
            Spacer(Modifier.width(3.dp))
            OreText(
                tab.name,
                Modifier.weight(1f, fill = false),
                color = if (selected) colors[BdColors.accentDeep] else colors[BdColors.text],
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (editing) {
                Spacer(Modifier.width(3.dp))
                BdIcon(BdIcons.Pencil, color = colors[BdColors.accent])
            }
        }
    }
}

/**
 * 分类编辑区：图标与名称、搜索式、特定补充，以及所选资源能写进搜索式的条件。
 * [enabled] 为 false 时不响应
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CategoryEditorPanel(
    editor: CategoryEditor,
    send: (StorageAction) -> Unit,
    text: StorageText,
    enabled: Boolean,
) {
    val colors = Bd.colors
    BdSectionLabel(text.editorTitle) {
        BdGlyphButton(BdIcons.Close, null, { send(StorageAction.CloseEditor) }, size = 11.dp)
    }
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BdTooltip(text.iconHint) {
            MarkerBox(editor.icon, null, selected = false, enabled) { right -> send(StorageAction.CategoryIcon(right)) }
        }
        Spacer(Modifier.width(4.dp))
        val name = rememberLocalText(editor.name)
        BdTextField(
            name.value,
            { name.edit(it) { value -> send(StorageAction.RenameCategory(value)) } },
            text.name,
            Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(7.dp))
    BdSectionLabel(text.rule)
    Spacer(Modifier.height(3.dp))
    val search = rememberLocalText(editor.search)
    BdTextArea(
        search.value,
        { search.edit(it) { value -> send(StorageAction.CategorySearch(value)) } },
        text.rulePlaceholder,
        minLines = 4,
        help = { SearchHelp(text, text.ruleHelp) },
    )
    Spacer(Modifier.height(2.dp))
    OreText(text.matches.format(editor.matches), color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
    Spacer(Modifier.height(7.dp))
    BdSectionLabel(text.keys) { HintMark(text.keysHint) }
    Spacer(Modifier.height(3.dp))
    // 已有的特定补充，末尾一格放入新的
    Box(Modifier.bdSlotFrame(colors)) {
        Column {
            for (row in (0..editor.keys.size).chunked(EDITOR_COLUMNS)) {
                Row {
                    for (index in row) {
                        val key = editor.keys.getOrNull(index)
                        MarkerBox(key?.icon, key?.name, selected = index == editor.inspected, enabled) { right ->
                            send(StorageAction.CategoryKeyClick(index, right))
                        }
                    }
                }
            }
        }
    }
    if (editor.chips.isNotEmpty()) {
        Spacer(Modifier.height(7.dp))
        BdSectionLabel(text.syntax) { HintMark(text.syntaxHint) }
        Spacer(Modifier.height(3.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (chip in editor.chips) SyntaxChip(chip, enabled) { send(StorageAction.AppendToSearch(chip)) }
        }
    }
}

/** 分组标题末尾的说明：悬停时显示 [hint] */
@Composable
private fun HintMark(hint: String) {
    BdTooltip(hint) { BdIcon(BdIcons.Info, color = Bd.colors[BdColors.faint]) }
}

/** 编辑区里的标记格：左右键分别响应，[name] 是悬停提示 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MarkerBox(
    icon: ItemIcon?,
    name: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: (right: Boolean) -> Unit
) {
    val colors = Bd.colors
    var hovered by remember { mutableStateOf(false) }
    BdTooltip(name.orEmpty(), enabled = name != null) {
        Box(
            Modifier.size(SLOT_PITCH.dp)
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .onPointerEvent(PointerEventType.Press) {
                    if (!enabled) return@onPointerEvent
                    when {
                        it.buttons.isPrimaryPressed -> onClick(false)
                        it.buttons.isSecondaryPressed -> onClick(true)
                    }
                }
        ) {
            OreSlot(Modifier.matchParentSize(), contentModifier = Modifier.size(16.dp)) {
                Box(Modifier.matchParentSize().background(colors[BdColors.accentSoft]))
                icon?.let { MinecraftItemIcon(it, Modifier.fillMaxSize()) }
            }
            if (selected || hovered) Box(Modifier.matchParentSize().brackets(colors[BdColors.marker], arm = 4.dp))
        }
    }
}

/** 一个可以加入搜索式的写法 */
@Composable
private fun SyntaxChip(syntax: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier.height(11.dp)
            .hoverable(interaction, enabled)
            .bdClickable(interaction, enabled = enabled, onClick = onClick)
            .background(if (hovered) colors[BdColors.accentSoft] else colors[BdColors.surface], Bd.ChipShape)
            .border(1.dp, if (hovered) colors[BdColors.accent] else colors[BdColors.line], Bd.ChipShape)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        OreText(
            syntax,
            color = if (hovered) colors[BdColors.accentDeep] else colors[BdColors.text],
            style = Bd.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 存储格子的右键菜单：取出一个、取出指定数量，以及把这种资源加入某个分类 */
@Composable
private fun SlotMenuPopup(menu: SlotMenu, point: IntOffset, send: (StorageAction) -> Unit, text: StorageText) {
    val colors = Bd.colors
    var amount by remember { mutableIntStateOf((menu.takeable + 1) / 2) }
    BdPointMenu(point, { send(StorageAction.CloseSlotMenu) }) {
        OreText(
            menu.name,
            Modifier.widthIn(max = 120.dp).padding(horizontal = 5.dp, vertical = 2.dp),
            color = colors[BdColors.faint],
            style = Bd.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (menu.takeable > 0) {
            BdMenuRow(text.takeOne) { send(StorageAction.TakeFromMenu(1)) }
            if (menu.takeable > 1) TakeAmountRow(amount, { amount = it }, menu.takeable, text) {
                send(StorageAction.TakeFromMenu(amount))
            }
        }
        if (menu.categories.isNotEmpty()) {
            if (menu.takeable > 0) {
                Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(colors[BdColors.line]))
            }
            OreText(
                text.addTo,
                Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                color = colors[BdColors.faint],
                style = Bd.caption,
                maxLines = 1
            )
            // 分类很多时在菜单里滚动
            BdScrollColumn(Modifier.heightIn(max = (13 * MENU_CATEGORY_ROWS).dp)) {
                for ((index, name) in menu.categories.withIndex()) {
                    BdMenuRow(name) { send(StorageAction.AddToCategory(index)) }
                }
            }
        }
    }
}

/** 右键菜单最多同时列出的分类数，再多就在菜单里滚动 */
private const val MENU_CATEGORY_ROWS = 8

/** 菜单里的"取出 x 个"：点击这一行就取出，数量编辑器自己处理的点击除外 */
@Composable
private fun TakeAmountRow(amount: Int, onAmountChange: (Int) -> Unit, max: Int, text: StorageText, onTake: () -> Unit) {
    val colors = Bd.colors
    val focus = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val color = if (hovered) colors[BdColors.accentDeep] else colors[BdColors.text]
    Row(
        Modifier.fillMaxWidth()
            .height(17.dp)
            .hoverable(interaction)
            .bdClickable(interaction) {
                focus.clearFocus()
                onTake()
            }
            .background(if (hovered) colors[BdColors.accentSoft] else Color.Transparent)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OreText(text.take, color = color, style = Bd.caption, maxLines = 1)
        Spacer(Modifier.width(3.dp))
        BdNumberEditor(amount, onAmountChange, 1..max)
        if (text.takeUnit.isNotEmpty()) {
            Spacer(Modifier.width(3.dp))
            OreText(text.takeUnit, color = color, style = Bd.caption, maxLines = 1)
        }
    }
}

/** 排序方式：主要与次要排序各一列，底部是倒序开关 */
@Composable
private fun SortOptions(state: StorageState, send: (StorageAction) -> Unit, text: StorageText) {
    Row {
        Column(Modifier.width(IntrinsicSize.Max)) {
            BdSectionLabel(text.sort)
            Spacer(Modifier.height(3.dp))
            for (policy in StorageSort.entries) {
                OptionRow(text.sortNames.getValue(policy), state.sort == policy) { send(StorageAction.Sort(policy)) }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.width(IntrinsicSize.Max)) {
            BdSectionLabel(text.secondarySort)
            Spacer(Modifier.height(3.dp))
            OptionRow(text.none, state.secondarySort == null) { send(StorageAction.SecondarySort(null)) }
            for (policy in StorageSort.entries) {
                OptionRow(
                    text.sortNames.getValue(policy),
                    state.secondarySort == policy,
                    enabled = policy != state.sort
                ) {
                    send(StorageAction.SecondarySort(policy))
                }
            }
        }
    }
    Spacer(Modifier.height(5.dp))
    BdSettingRow(text.reverse) { BdToggle(state.reverse, { send(StorageAction.Reverse(it)) }) }
}

/** 设置页的视图设置：行列与搜索行为；[shown] 为 false 时控件不响应 */
@Composable
private fun ViewOptions(state: StorageState, send: (StorageAction) -> Unit, text: StorageText, shown: Boolean) {
    BdSectionLabel(text.layout)
    BdSettingRow(text.columns) {
        BdNumberEditor(
            state.preferredColumns,
            { send(StorageAction.PreferredSize(it, state.preferredRows)) },
            9..99,
            shown
        )
    }
    BdSettingRow(text.rows) {
        BdNumberEditor(
            state.preferredRows,
            { send(StorageAction.PreferredSize(state.preferredColumns, it)) },
            2..99,
            shown
        )
    }
    BdSectionDivider()
    BdSectionLabel(text.interaction)
    BdSettingRow(text.rightClickMenu, text.rightClickMenuHint) {
        BdToggle(state.rightClickMenu, { send(StorageAction.RightClickMenu(it)) }, shown)
    }
    BdSectionDivider()
    BdSectionLabel(text.searchTitle)
    BdSettingRow(text.keepSearch) { BdToggle(state.keepSearch, { send(StorageAction.KeepSearch(it)) }, shown) }
    BdSettingRow(text.syncSearch, text.syncSearchHint) {
        BdToggle(
            state.syncSearch,
            { send(StorageAction.SyncSearch(it)) },
            shown
        )
    }
    BdSectionDivider()
    BdSectionLabel(text.workbench)
    BdSettingRow(text.keepCrafting, text.keepCraftingHint) {
        BdToggle(state.keepCrafting, { send(StorageAction.KeepCrafting(it)) }, shown)
    }
}

/** 浮层中的单选项 */
@Composable
internal fun OptionRow(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .height(13.dp)
            .hoverable(interaction, enabled)
            .bdClickable(interaction, enabled = enabled && !selected, onClick = onClick)
            .background(if (hovered && enabled && !selected) colors[BdColors.accentSoft] else Color.Transparent)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(3.dp).background(if (selected) colors[BdColors.accent] else Color.Transparent))
        Spacer(Modifier.width(4.dp))
        OreText(
            label,
            color =
                when {
                    !enabled -> colors[BdColors.faint]
                    selected -> colors[BdColors.accentDeep]
                    else -> colors[BdColors.text]
                },
            maxLines = 1,
        )
    }
}
