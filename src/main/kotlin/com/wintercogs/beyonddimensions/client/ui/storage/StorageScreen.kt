package com.wintercogs.beyonddimensions.client.ui.storage

import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.wintercogs.beyonddimensions.client.init.BDShortKeys
import com.wintercogs.beyonddimensions.client.ui.base.*
import com.wintercogs.beyonddimensions.client.ui.kit.*
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import com.wintercogs.beyonddimensions.client.ui.theme.signature
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.item.MinecraftItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.inventory.OreSlot
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.Screen
import kotlin.math.roundToInt
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

/** 界面不会变化的部分：槽位编号、图标与文字，在游戏线程构造 */
class StorageLayout(menu: DimensionsNetMenu) {
    val storageStart = menu.storageStartIndex
    val playerSlots = (menu.inventoryStartIndex until menu.inventoryEndIndex).toList()
    val craftSlots =
        (menu as? DimensionsCraftMenu)?.let { (it.craftSlotStartIndex until it.craftSlotEndIndex).toList() }.orEmpty()
    val resultSlot = (menu as? DimensionsCraftMenu)?.resultSlotIndex ?: -1
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_TERMINAL_BLOCK.get()))
    val showSwitcher = !ClientConfigRuntime.disableMultiNetworkSwitching
    val text = StorageText()
}

class StorageText {
    val search = tr("ui.beyonddimensions.storage.search")
    val searchTitle = tr("ui.beyonddimensions.storage.search.title")
    val searchHelp = tr("ui.beyonddimensions.storage.search.help")

    /** 搜索写法：每项一个示例与它的含义 */
    val searchSyntax =
        listOf("name", "mod", "tag", "tooltip", "id", "durability", "components", "exclude", "any").map {
            tr("ui.beyonddimensions.storage.search.help.$it") to tr("ui.beyonddimensions.storage.search.help.$it.hint")
        }
    val searchRule = tr("ui.beyonddimensions.storage.search.help.rule")
    val addCategory = tr("ui.beyonddimensions.storage.category.add")
    val categoryList = tr("ui.beyonddimensions.storage.category.list")
    val edit = tr("ui.beyonddimensions.storage.category.edit")
    val delete = tr("ui.beyonddimensions.storage.category.delete")
    val moveLeft = tr("ui.beyonddimensions.storage.category.move_left")
    val moveRight = tr("ui.beyonddimensions.storage.category.move_right")
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
    val crafting = tr("ui.beyonddimensions.storage.crafting")
    val switcher = tr("ui.beyonddimensions.storage.switch_network")
    val view = tr("ui.beyonddimensions.storage.view")
    val sort = tr("ui.beyonddimensions.storage.sort")
    val secondarySort = tr("ui.beyonddimensions.storage.secondary_sort")
    val none = tr("ui.beyonddimensions.storage.sort.none")
    val reverse = tr("ui.beyonddimensions.storage.reverse")
    val layout = tr("ui.beyonddimensions.storage.layout")
    val columns = tr("ui.beyonddimensions.storage.columns")
    val rows = tr("ui.beyonddimensions.storage.rows")
    val keepSearch = tr("ui.beyonddimensions.storage.keep_search")
    val syncSearch = tr("ui.beyonddimensions.storage.sync_search")
    val syncSearchHint = tr("ui.beyonddimensions.storage.sync_search.hint")
    val inventory = tr("ui.beyonddimensions.inventory")
    val clearToStorage = tr("ui.beyonddimensions.crafting.clear_to_storage")
    val clearToInventory = tr("ui.beyonddimensions.crafting.clear_to_inventory")
    val returnTitle = tr("ui.beyonddimensions.crafting.return")
    val returnOnClear = tr("ui.beyonddimensions.crafting.return.clear")
    val keepCrafting = tr("ui.beyonddimensions.crafting.keep")
    val keepCraftingHint = tr("ui.beyonddimensions.crafting.keep.hint")
    val returnStorage = tr("ui.beyonddimensions.crafting.return.storage")
    val returnInventory = tr("ui.beyonddimensions.crafting.return.inventory")
    val recipe = tr("ui.beyonddimensions.crafting.recipe")
    val sortNames = SORT_POLICIES.associateWith {
        tr(
            "ui.beyonddimensions.storage.sort.${
                it.name.removePrefix("SORT_").lowercase()
            }"
        )
    }
}

/** 存储终端与合成终端 */
class StorageScreen(private val storageMenu: DimensionsNetMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<DimensionsNetMenu, StorageState, StorageAction>(storageMenu, title) {
    private val controller = StorageController(storageMenu)
    private val layout = StorageLayout(storageMenu)

    init {
        adapter.resourceMenu = controller::openSlotMenu
    }

    override fun snapshot() = controller.snapshot()

    override fun handle(action: StorageAction) = controller.handle(action)

    @Composable
    override fun Content(state: StorageState, slots: ComposeMenuSlots<DimensionsNetMenu>) =
        StorageView(state, ::send, ::requestClose, slots, layout, pages)

    override fun inventoryTick() {
        super.inventoryTick()
        controller.tick()
    }

    override fun menuClosed() {
        super.menuClosed()
        controller.saveSearch()
        controller.saveNow()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) storageMenu.hasShiftDown = true
        if (!hasTextInputFocus) {
            if (Screen.hasShiftDown() && keyCode == GLFW.GLFW_KEY_Z) {
                controller.toggleSearchSync()
                return true
            }
            if (BDShortKeys.OPEN_GUI_KEY.matches(keyCode, scanCode)) {
                onClose()
                return true
            }
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

/** 分区之间的间距 */
private const val GAP = 7
private const val SCREEN_MARGIN = 8

/** 分组标题的行高：9sp 小字的行高 */
private const val LABEL = 11

/** 格子区域（含边框）、间隔与滚动条的总宽度 */
private fun contentWidth(columns: Int) = columns * SLOT_PITCH + 2 + 2 + SCROLLBAR_WIDTH

/** 窗口中除格子区域外的宽度：页签竖条、页面两侧的留白与窗口边框 */
private const val CHROME_WIDTH = SIDE_RAIL_WIDTH + PAGE_PADDING_X * 2 + 2

/** 分类编辑区：与主内容的间隔、分隔线、间隔与七列标记格 */
private const val EDITOR_COLUMNS = 7
private const val EDITOR_CONTENT_WIDTH = EDITOR_COLUMNS * SLOT_PITCH + 1
private const val EDITOR_WIDTH = GAP + 1 + GAP + EDITOR_CONTENT_WIDTH
private const val EDITOR_MILLIS = 180

/** 除格子行外的固定高度。分类标签栏始终计入：空间不够时少放几行格子，标签栏不会被挤掉 */
private fun chromeHeight(craft: Boolean): Int {
    var height = 25 + PAGE_PADDING_TOP + 18 + 5 + 13 + 4 + 2 + GAP + LABEL + 4 + 76 + PAGE_PADDING_BOTTOM + 2
    if (craft) height += GAP + LABEL + 4 + 54
    return height
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
) {
    val colors = Bd.colors
    val text = layout.text
    // 方块终端可以在本地隐藏合成区；可切换的终端则重新打开对应的菜单
    var craftHidden by remember { mutableStateOf(false) }
    val craftShown = state.craft != null && !craftHidden
    val page = rememberPageSelection()
    val settingsOpen = page.settings
    // 排序与配方选择的浮层盖在槽位上方，打开期间停用槽位
    val popovers = remember { BdPopovers() }
    slots.Interaction(enabled = popovers.open == 0)

    CompositionLocalProvider(LocalBdPopovers provides popovers) {
        BdScreenFrame { available ->
            val availableHeight = available.height.value.toInt() - SCREEN_MARGIN * 2
            val availableWidth = available.width.value.toInt() - SCREEN_MARGIN * 2
            val fitRows = (availableHeight - chromeHeight(craftShown)) / SLOT_PITCH
            // 内容区居中，竖条对面也留出一条竖条宽的空位；分类编辑区打开时窗口向右扩展
            val editorOpen = state.editor != null
            val fitColumns =
                (availableWidth - CHROME_WIDTH - SIDE_RAIL_WIDTH - contentWidth(0) -
                    if (editorOpen) EDITOR_WIDTH else 0) / SLOT_PITCH
            val rows = fitRows.coerceAtMost(state.preferredRows).coerceAtLeast(2)
            val columns = fitColumns.coerceAtMost(state.preferredColumns).coerceAtLeast(9)
            LaunchedEffect(columns, rows) { send(StorageAction.Viewport(columns, rows)) }

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
                Modifier.width((CHROME_WIDTH + width + editorWidth).dp).then(slots.areaModifier())
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        // 分类标签与编辑区自己滚动时不再翻动格子
                        if (event.changes.any { it.isConsumed }) return@onPointerEvent
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (delta != 0f && page.main) send(StorageAction.Scroll(if (delta > 0) 1 else -1))
                    },
                header = {
                    BdHeader(
                        layout.icon,
                        state.networkName.ifEmpty { text.storage },
                        close,
                        tag = if (state.networkId >= 0) "#%04d".format(state.networkId) else null,
                    )
                },
                // 存储与合成两个工作区都在主页面上，合成区显示与否决定选中哪一个
                rail = {
                    BdRailTab(text.storage, BdGlyphs.Storage, selected = page.main && !craftShown) {
                        page.open(BdPages.MAIN)
                        if (craftShown) {
                            if (state.craft.toggleable) send(StorageAction.ToggleCraft) else craftHidden = true
                        }
                    }
                    BdRailTab(text.crafting, BdGlyphs.Crafting, selected = page.main && craftShown) {
                        page.open(BdPages.MAIN)
                        if (!craftShown) {
                            if (state.craft == null) send(StorageAction.ToggleCraft) else craftHidden = false
                        }
                    }
                    InjectedTabs(pages, page)
                    if (layout.showSwitcher) {
                        BdGlyphButton(
                            OreGlyph.CycleArrows,
                            text.switcher,
                            { send(StorageAction.OpenSwitcher) },
                            size = 20.dp,
                            glyphSize = 9.dp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    BdRailTab(text.view, OreGlyph.Gear.art, selected = settingsOpen) { page.open(BdPages.SETTINGS) }
                },
            ) {
                BdMainPage(page.main) {
                    Toolbar(state, send, text, width)
                    Spacer(Modifier.height(5.dp))
                    CategoryTabs(state, send, text, width)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.height((gridRows * SLOT_PITCH + 2).dp)) {
                        // 右键菜单在按下右键的位置展开
                        var pressAt by remember { mutableStateOf(IntOffset.Zero) }
                        Box(
                            Modifier.width((gridColumns * SLOT_PITCH + 2).dp).fillMaxHeight()
                                .background(colors[BdColors.surface])
                                .border(1.dp, colors[BdColors.line]).padding(1.dp)
                                .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) {
                                    pressAt = it.changes.first().position.round()
                                }
                        ) {
                            val ids = List(gridColumns * gridRows) { layout.storageStart + it }
                            BdSlotGrid(slots, ids, gridColumns)
                            state.slotMenu?.let { menu -> key(menu.serial) { SlotMenuPopup(menu, pressAt, send, text) } }
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
                    if (craftShown) {
                        Spacer(Modifier.height(GAP.dp))
                        CraftSection(state.craft, state.keepCrafting, send, slots, layout, width)
                    }
                    Spacer(Modifier.height(GAP.dp))
                    BdSectionLabel(text.inventory, Modifier.width(width.dp))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.width((gridColumns * SLOT_PITCH + 2).dp), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.background(colors[BdColors.line]).padding(0.5.dp)) {
                            BdPlayerInventory(
                                slots,
                                layout.playerSlots
                            )
                        }
                    }
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

@Composable
private fun Toolbar(state: StorageState, send: (StorageAction) -> Unit, text: StorageText, width: Int) {
    // 配方查看器改动搜索时同步进来
    val search = rememberLocalText(state.search)
    val colors = Bd.colors
    Row(Modifier.width(width.dp), verticalAlignment = Alignment.CenterVertically) {
        BdSearchField(
            search.value,
            { search.edit(it) { value -> send(StorageAction.Search(value)) } },
            text.search,
            Modifier.weight(1f),
            help = { SearchHelp(text) },
        )
        Spacer(Modifier.width(4.dp))
        var open by remember { mutableStateOf(false) }
        Box {
            BdChip({ open = !open }, active = open) {
                OreIcon(OreGlyph.Bars, Modifier.size(7.dp), color = colors[BdColors.faint])
                Spacer(Modifier.width(3.dp))
                OreText(text.sortNames[state.sort] ?: text.sort, color = colors[BdColors.text], maxLines = 1)
                Spacer(Modifier.width(3.dp))
                OreIcon(
                    if (state.reverse) OreGlyph.ArrowUp else OreGlyph.ArrowDown,
                    Modifier.size(7.dp),
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
    Row {
        Column(Modifier.width(IntrinsicSize.Max)) {
            for ((example, _) in text.searchSyntax) {
                OreText(example, color = colors[BdColors.accentDeep], style = Bd.caption, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            for ((_, meaning) in text.searchSyntax) OreText(
                meaning,
                color = colors[BdColors.muted],
                style = Bd.caption,
                maxLines = 1
            )
        }
    }
    OreText(rule, color = colors[BdColors.faint], style = Bd.caption)
}

/**
 * 输入框的本地文字，不必等一刻的往返。一刻内输入多个字（快速输入、输入法上屏一个词）时，快照会依次带回较早发出的文字，
 * 这些不算外部改动；外部改动（如配方查看器的搜索、点选的写法）再同步进来
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
 * 分类标签：选中项下方是标志渐变线，编辑区打开的分类带一支笔；标签后是新建按钮，最右侧的下拉列出全部标签。
 * 放不下时用滚轮横向滚动，切换到或开始编辑的标签会滚进视野；右键打开编辑、移动与删除的菜单，也可以直接拖动排序
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

    // 新加的标签要等一帧排好位置。直接跳到位：两次切换接连发生时，第二次按第一次的终点算
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
                OreGlyph.Plus,
                text.addCategory,
                { send(StorageAction.AddCategory) },
                size = 11.dp,
                glyphSize = 7.dp
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
                    MinecraftItemIcon(tab.icon, Modifier.size(9.dp))
                    Spacer(Modifier.width(2.dp))
                }
                OreText(
                    tab.name,
                    color = if (selected || hovered) colors[BdColors.text] else colors[BdColors.muted],
                    maxLines = 1
                )
                if (editing) {
                    Spacer(Modifier.width(2.dp))
                    OreIcon(OreGlyph.Pencil, Modifier.size(7.dp), color = colors[BdColors.accent])
                }
            }
            Spacer(Modifier.weight(1f))
            if (selected) Box(Modifier.fillMaxWidth().height(2.dp).background(colors.signature))
        }
        menuAt?.let { point ->
            val dismiss = { menuAt = null }
            BdPointMenu(point, dismiss) {
                BdMenuRow(text.edit) {
                    dismiss()
                    send(StorageAction.EditCategory(index))
                }
                if (index > 0) BdMenuRow(text.moveLeft) {
                    dismiss()
                    send(StorageAction.MoveCategory(index, index - 1))
                }
                if (index < count - 1) BdMenuRow(text.moveRight) {
                    dismiss()
                    send(StorageAction.MoveCategory(index, index + 1))
                }
                BdMenuRow(text.delete) {
                    dismiss()
                    send(StorageAction.DeleteCategory(index))
                }
            }
        }
    }
}

/** 下拉列出全部标签：标签多到一行放不下时，直接从这里挑选 */
@Composable
private fun CategoryList(state: StorageState, editing: Int, send: (StorageAction) -> Unit, text: StorageText) {
    var open by remember { mutableStateOf(false) }
    Box {
        BdGlyphButton(
            OreGlyph.ChevronDown,
            text.categoryList,
            { open = !open },
            enabled = state.categories.isNotEmpty(),
            selected = open,
            size = 11.dp,
            glyphSize = 7.dp,
        )
        if (open) {
            BdPopover({ open = false }) {
                BdScrollColumn(Modifier.widthIn(max = 160.dp).heightIn(max = 150.dp)) {
                    state.categories.forEachIndexed { index, tab ->
                        CategoryOption(tab, selected = index == state.category, editing = index == editing) {
                            send(StorageAction.SelectCategory(index))
                            open = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryOption(tab: CategoryTab, selected: Boolean, editing: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth()
            .height(13.dp)
            .hoverable(interaction)
            .bdClickable(interaction, enabled = !selected, onClick = onClick)
            .background(if (hovered && !selected) colors[BdColors.accentSoft] else Color.Transparent)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(3.dp).background(if (selected) colors[BdColors.accent] else Color.Transparent))
        Spacer(Modifier.width(4.dp))
        // 没有图标的分类显示一个漏斗作占位
        Box(Modifier.size(9.dp), contentAlignment = Alignment.Center) {
            if (tab.icon != null) MinecraftItemIcon(tab.icon, Modifier.fillMaxSize())
            else OreIcon(OreGlyph.Funnel, Modifier.size(7.dp), color = colors[BdColors.faint])
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
            OreIcon(OreGlyph.Pencil, Modifier.size(7.dp), color = colors[BdColors.accent])
        }
    }
}

/**
 * 分类编辑区：图标与名称、搜索式、特定补充，以及所选资源能写进搜索式的条件。
 * [enabled] 为 false 时编辑区正在收起或被设置页盖住，不再响应
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
        BdGlyphButton(OreGlyph.Cross, null, { send(StorageAction.CloseEditor) }, size = 11.dp, glyphSize = 6.dp)
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
    Box(Modifier.background(colors[BdColors.line]).padding(0.5.dp)) {
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
    BdTooltip(hint) { OreIcon(OreGlyph.InformationCircle, Modifier.size(7.dp), color = Bd.colors[BdColors.faint]) }
}

/** 编辑区里的标记格：外观同存储格子，左右键分别响应；[name] 是悬停提示 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MarkerBox(icon: ItemIcon?, name: String?, selected: Boolean, enabled: Boolean, onClick: (right: Boolean) -> Unit) {
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
            OreSlot(Modifier.matchParentSize(), highlighted = hovered, contentModifier = Modifier.size(16.dp)) {
                Box(Modifier.matchParentSize().background(colors[BdColors.accentSoft]))
                icon?.let { MinecraftItemIcon(it, Modifier.fillMaxSize()) }
            }
            if (selected || hovered) Box(Modifier.matchParentSize().brackets(colors[BdColors.accent], arm = 4.dp))
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

/** 存储格子的右键菜单：取出一个、取出指定数量（默认一半），以及把这种资源加入某个分类 */
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
            // 分类很多时在菜单里滚动，菜单不会长出屏幕
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

/**
 * 菜单里的"取出 x 个"：与"取出一个"一样，点击这一行就取出。数量编辑器的按钮与输入框自己处理点击，不会取出；
 * 点击时先让正在输入的数字提交
 */
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

/** 合成区：3×3 合成格、产物与退回按钮 */
@Composable
private fun CraftSection(
    craft: CraftState,
    keep: Boolean,
    send: (StorageAction) -> Unit,
    slots: ComposeMenuSlots<DimensionsNetMenu>,
    layout: StorageLayout,
    width: Int
) {
    val colors = Bd.colors
    val text = layout.text
    Column(Modifier.width(width.dp)) {
        BdSectionLabel(text.crafting) {
            BdGlyphButton(
                BdGlyphs.ToStorage,
                text.clearToStorage,
                { send(StorageAction.ClearCraft(true)) },
                size = 11.dp,
                glyphSize = 8.dp
            )
            Spacer(Modifier.width(2.dp))
            BdGlyphButton(
                BdGlyphs.ToInventory,
                text.clearToInventory,
                { send(StorageAction.ClearCraft(false)) },
                size = 11.dp,
                glyphSize = 8.dp
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(colors[BdColors.line]).padding(0.5.dp)) { BdSlotGrid(slots, layout.craftSlots, 3) }
            Spacer(Modifier.width(7.dp))
            OreIcon(OreGlyph.ArrowRight, Modifier.size(8.dp), color = colors[BdColors.accent])
            Spacer(Modifier.width(7.dp))
            // 产物槽带一圈强调色细框
            Box(Modifier.border(1.dp, colors[BdColors.accent]).padding(2.dp)) { BdSlot(slots, layout.resultSlot) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                // 保留合成格时关闭不再退回，退回方向只在换入配方清空合成格时用到
                OreText(
                    if (keep) text.returnOnClear else text.returnTitle,
                    color = colors[BdColors.faint],
                    style = Bd.caption,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                BdSegmented(
                    listOf(text.returnStorage, text.returnInventory),
                    if (craft.returnToStorage) 0 else 1,
                    { send(StorageAction.ReturnPreference(it == 0)) },
                )
                if (craft.recipeChoices.size > 1) {
                    Spacer(Modifier.height(4.dp))
                    RecipeChooser(craft.recipeChoices, send, text)
                }
            }
        }
    }
}

@Composable
private fun RecipeChooser(choices: List<RecipeChoice>, send: (StorageAction) -> Unit, text: StorageText) {
    val colors = Bd.colors
    var open by remember { mutableStateOf(false) }
    Box {
        BdChip({ open = !open }, active = open) {
            OreText(text.recipe, color = colors[BdColors.faint], style = Bd.caption, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            OreText(choices.size.toString(), color = colors[BdColors.accentDeep], maxLines = 1)
            Spacer(Modifier.width(3.dp))
            OreIcon(OreGlyph.ChevronRight, Modifier.size(6.dp), color = colors[BdColors.muted])
        }
        if (open) {
            BdPopover({ open = false }) {
                for (choice in choices) {
                    OptionRow(choice.label, selected = false) {
                        send(StorageAction.ChooseRecipe(choice.id))
                        open = false
                    }
                }
            }
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
            for (policy in SORT_POLICIES) {
                OptionRow(text.sortNames.getValue(policy), state.sort == policy) { send(StorageAction.Sort(policy)) }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.width(IntrinsicSize.Max)) {
            BdSectionLabel(text.secondarySort)
            Spacer(Modifier.height(3.dp))
            OptionRow(text.none, state.secondarySort == null) { send(StorageAction.SecondarySort(null)) }
            for (policy in SORT_POLICIES) {
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

/** 设置页的视图设置：行列与搜索行为；[shown] 为 false 时设置页正在淡出，控件不再响应 */
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
    BdSectionLabel(text.searchTitle)
    BdSettingRow(text.keepSearch) { BdToggle(state.keepSearch, { send(StorageAction.KeepSearch(it)) }, shown) }
    BdSettingRow(text.syncSearch, text.syncSearchHint) {
        BdToggle(
            state.syncSearch,
            { send(StorageAction.SyncSearch(it)) },
            shown
        )
    }
    BdSectionLabel(text.crafting)
    BdSettingRow(text.keepCrafting, text.keepCraftingHint) {
        BdToggle(state.keepCrafting, { send(StorageAction.KeepCrafting(it)) }, shown)
    }
}

/** 浮层中的单选项 */
@Composable
private fun OptionRow(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
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
