package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.init.BDShortKeys
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen
import com.wintercogs.beyonddimensions.client.ui.base.BdPlayerInventory
import com.wintercogs.beyonddimensions.client.ui.base.BdSlot
import com.wintercogs.beyonddimensions.client.ui.base.BdSlotGrid
import com.wintercogs.beyonddimensions.client.ui.base.SLOT_PITCH
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.client.ui.kit.BdChip
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphButton
import com.wintercogs.beyonddimensions.client.ui.kit.BdGlyphs
import com.wintercogs.beyonddimensions.client.ui.kit.BdHeader
import com.wintercogs.beyonddimensions.client.ui.kit.BdMainPage
import com.wintercogs.beyonddimensions.client.ui.kit.BdMeter
import com.wintercogs.beyonddimensions.client.ui.kit.BdPopover
import com.wintercogs.beyonddimensions.client.ui.kit.BdRailTab
import com.wintercogs.beyonddimensions.client.ui.kit.BdScreenFrame
import com.wintercogs.beyonddimensions.client.ui.kit.BdScrollbar
import com.wintercogs.beyonddimensions.client.ui.kit.BdSearchField
import com.wintercogs.beyonddimensions.client.ui.kit.BdSectionLabel
import com.wintercogs.beyonddimensions.client.ui.kit.BdSegmented
import com.wintercogs.beyonddimensions.client.ui.kit.BdSettingRow
import com.wintercogs.beyonddimensions.client.ui.kit.BdStepper
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabPage
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabbedWindow
import com.wintercogs.beyonddimensions.client.ui.kit.BdToggle
import com.wintercogs.beyonddimensions.client.ui.kit.PAGE_PADDING_BOTTOM
import com.wintercogs.beyonddimensions.client.ui.kit.PAGE_PADDING_TOP
import com.wintercogs.beyonddimensions.client.ui.kit.PAGE_PADDING_X
import com.wintercogs.beyonddimensions.client.ui.kit.SIDE_RAIL_WIDTH
import com.wintercogs.beyonddimensions.client.ui.kit.bdClickable
import com.wintercogs.beyonddimensions.client.ui.kit.formatExact
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.init.BDBlocks
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.ui.ore.display.OreGlyph
import dev.compixel.ui.ore.display.OreIcon
import dev.compixel.ui.ore.display.OreText
import dev.compixel.ui.ore.overlay.OreTooltip
import dev.compixel.ui.ore.overlay.OreTooltipMode
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW

/** 界面不会变化的部分：槽位编号、图标与文字，在游戏线程构造 */
class StorageLayout(menu: DimensionsNetMenu) {
    val storageStart = menu.storageStartIndex
    val playerSlots = (menu.inventoryStartIndex until menu.inventoryEndIndex).toList()
    val craftSlots = (menu as? DimensionsCraftMenu)?.let { (it.craftSlotStartIndex until it.craftSlotEndIndex).toList() }.orEmpty()
    val resultSlot = (menu as? DimensionsCraftMenu)?.resultSlotIndex ?: -1
    val icon = ItemIcon.snapshot(ItemStack(BDBlocks.NET_TERMINAL_BLOCK.get()))
    val showSwitcher = !ClientConfigRuntime.disableMultiNetworkSwitching
    val text = StorageText()
}

class StorageText {
    val search = tr("ui.beyonddimensions.storage.search")
    val searchHelp = tr("ui.beyonddimensions.storage.search.help")
    val all = tr("ui.beyonddimensions.storage.all")
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
    val types = tr("ui.beyonddimensions.storage.types")
    val filtered = tr("ui.beyonddimensions.storage.filtered")
    val inventory = tr("ui.beyonddimensions.inventory")
    val clearToStorage = tr("ui.beyonddimensions.crafting.clear_to_storage")
    val clearToInventory = tr("ui.beyonddimensions.crafting.clear_to_inventory")
    val returnTitle = tr("ui.beyonddimensions.crafting.return")
    val returnStorage = tr("ui.beyonddimensions.crafting.return.storage")
    val returnInventory = tr("ui.beyonddimensions.crafting.return.inventory")
    val recipe = tr("ui.beyonddimensions.crafting.recipe")
    val sortNames = SORT_POLICIES.associateWith { tr("ui.beyonddimensions.storage.sort.${it.name.removePrefix("SORT_").lowercase()}") }
}

/** 存储终端与合成终端 */
class StorageScreen(private val storageMenu: DimensionsNetMenu, inventory: Inventory, title: Component) :
    BdInventoryScreen<DimensionsNetMenu, StorageState, StorageAction>(storageMenu, title) {
    private val controller = StorageController(storageMenu)
    private val layout = StorageLayout(storageMenu)

    override fun snapshot() = controller.snapshot()

    override fun handle(action: StorageAction) = controller.handle(action)

    @Composable
    override fun Content(state: StorageState, slots: ComposeMenuSlots<DimensionsNetMenu>) =
        StorageView(state, ::send, ::requestClose, slots, layout)

    override fun menuClosed() {
        super.menuClosed()
        controller.saveSearch()
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
private const val SCROLLBAR = 5
private const val SCREEN_MARGIN = 8
/** 分组标题与状态栏的行高：9sp 小字的行高 */
private const val LABEL = 11

/** 格子区域（含边框）、间隔与滚动条的总宽度 */
private fun contentWidth(columns: Int) = columns * SLOT_PITCH + 2 + 2 + SCROLLBAR

/** 窗口中除格子区域外的宽度：页签竖条、页面两侧的留白与窗口边框 */
private const val CHROME_WIDTH = SIDE_RAIL_WIDTH + PAGE_PADDING_X * 2 + 2

/** 除格子行外的固定高度 */
private fun chromeHeight(tabs: Boolean, status: Boolean, craft: Boolean): Int {
    var height = 25 + PAGE_PADDING_TOP + 18 + 4 + 2 + GAP + LABEL + 4 + 76 + PAGE_PADDING_BOTTOM + 2
    if (tabs) height += 5 + 13
    if (status) height += 4 + LABEL
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
) {
    val colors = Bd.colors
    val text = layout.text
    // 方块终端可以在本地隐藏合成区；可切换的终端则重新打开对应的菜单
    var craftHidden by remember { mutableStateOf(false) }
    val craftShown = state.craft != null && !craftHidden
    var settingsOpen by remember { mutableStateOf(false) }
    slots.Interaction(enabled = !settingsOpen)

    BdScreenFrame { available ->
        val availableHeight = available.height.value.toInt() - SCREEN_MARGIN * 2
        val availableWidth = available.width.value.toInt() - SCREEN_MARGIN * 2
        // 空间不足三行时收起页签与状态栏
        val roomy = (availableHeight - chromeHeight(tabs = true, status = true, craft = craftShown)) / SLOT_PITCH >= 3
        val fitRows = (availableHeight - chromeHeight(roomy, roomy, craftShown)) / SLOT_PITCH
        // 内容区居中，竖条对面也留出一条竖条宽的空位
        val fitColumns = (availableWidth - CHROME_WIDTH - SIDE_RAIL_WIDTH - contentWidth(0)) / SLOT_PITCH
        val rows = fitRows.coerceAtMost(state.preferredRows).coerceAtLeast(2)
        val columns = fitColumns.coerceAtMost(state.preferredColumns).coerceAtLeast(9)
        LaunchedEffect(columns, rows) { send(StorageAction.Viewport(columns, rows)) }

        val gridColumns = state.columns
        val gridRows = state.rows
        val width = contentWidth(gridColumns)
        BdTabbedWindow(
            Modifier.width((CHROME_WIDTH + width).dp).then(slots.areaModifier()).onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (delta != 0f && !settingsOpen) send(StorageAction.Scroll(if (delta > 0) 1 else -1))
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
                BdRailTab(text.storage, BdGlyphs.Storage, selected = !settingsOpen && !craftShown) {
                    settingsOpen = false
                    if (craftShown) {
                        if (state.craft.toggleable) send(StorageAction.ToggleCraft) else craftHidden = true
                    }
                }
                BdRailTab(text.crafting, BdGlyphs.Crafting, selected = !settingsOpen && craftShown) {
                    settingsOpen = false
                    if (!craftShown) {
                        if (state.craft == null) send(StorageAction.ToggleCraft) else craftHidden = false
                    }
                }
                if (layout.showSwitcher) {
                    BdGlyphButton(OreGlyph.CycleArrows, text.switcher, { send(StorageAction.OpenSwitcher) }, size = 20.dp, glyphSize = 9.dp)
                }
                Spacer(Modifier.weight(1f))
                BdRailTab(text.view, OreGlyph.Gear.art, selected = settingsOpen) { settingsOpen = true }
            },
        ) {
            BdMainPage(!settingsOpen) {
                Toolbar(state, send, text, width)
                if (roomy) {
                    Spacer(Modifier.height(5.dp))
                    TypeTabs(state, send, text, width)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.height((gridRows * SLOT_PITCH + 2).dp)) {
                    Box(Modifier.width((gridColumns * SLOT_PITCH + 2).dp).fillMaxHeight().background(colors.surface).border(1.dp, colors.line).padding(1.dp)) {
                        val ids = List(gridColumns * gridRows) { layout.storageStart + it }
                        BdSlotGrid(slots, ids, gridColumns)
                    }
                    if (state.totalRows > gridRows) {
                        Spacer(Modifier.width(2.dp))
                        BdScrollbar(
                            state.firstRow,
                            gridRows,
                            state.totalRows,
                            { send(StorageAction.ScrollTo(it)) },
                            Modifier.width(SCROLLBAR.dp).fillMaxHeight(),
                        )
                    }
                }
                if (roomy) {
                    Spacer(Modifier.height(4.dp))
                    StatusStrip(state, text, width)
                }
                if (craftShown) {
                    Spacer(Modifier.height(GAP.dp))
                    CraftSection(state.craft, send, slots, layout, width)
                }
                Spacer(Modifier.height(GAP.dp))
                BdSectionLabel(text.inventory, Modifier.width(width.dp))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width((gridColumns * SLOT_PITCH + 2).dp), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.background(colors.line).padding(0.5.dp)) { BdPlayerInventory(slots, layout.playerSlots) }
                }
            }
            BdTabPage(settingsOpen) { ViewOptions(state, send, text, settingsOpen) }
        }
    }
}

@Composable
private fun Toolbar(state: StorageState, send: (StorageAction) -> Unit, text: StorageText, width: Int) {
    // 输入框使用本地状态，避免等待一刻的往返；外部（配方查看器）改动搜索时再同步进来
    var value by remember { mutableStateOf(state.search) }
    var sent by remember { mutableStateOf(state.search) }
    LaunchedEffect(state.search) {
        if (state.search != sent) {
            value = state.search
            sent = state.search
        }
    }
    val colors = Bd.colors
    Row(Modifier.width(width.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            OreTooltip(text.searchHelp, mode = OreTooltipMode.Immediate) {
                BdSearchField(
                    value,
                    {
                        value = it
                        sent = it
                        send(StorageAction.Search(it))
                    },
                    text.search,
                    Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        var open by remember { mutableStateOf(false) }
        Box {
            BdChip({ open = !open }, active = open) {
                OreIcon(OreGlyph.Bars, Modifier.size(7.dp), color = colors.faint)
                Spacer(Modifier.width(3.dp))
                OreText(text.sortNames[state.sort] ?: text.sort, color = colors.text, maxLines = 1)
                Spacer(Modifier.width(3.dp))
                OreIcon(if (state.reverse) OreGlyph.ArrowUp else OreGlyph.ArrowDown, Modifier.size(7.dp), color = colors.accent)
            }
            if (open) BdPopover({ open = false }) { SortOptions(state, send, text) }
        }
    }
}

/** 资源类型页签；选中项下方是标志渐变线 */
@Composable
private fun TypeTabs(state: StorageState, send: (StorageAction) -> Unit, text: StorageText, width: Int) {
    val colors = Bd.colors
    Row(
        Modifier.width(width.dp).height(13.dp).drawBehind {
            drawLine(colors.line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
        },
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TypeTab(text.all, state.stored, state.typeFilter == null) { send(StorageAction.Filter(null)) }
        for (tab in state.tabs) {
            TypeTab(tab.label, tab.count, state.typeFilter == tab.id) { send(StorageAction.Filter(tab.id)) }
        }
    }
}

@Composable
private fun TypeTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = Bd.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier.width(IntrinsicSize.Max)
            .fillMaxHeight()
            .hoverable(interaction)
            .bdClickable(interaction, enabled = !selected, onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            OreText(label, color = if (selected || hovered) colors.text else colors.muted, maxLines = 1)
            Spacer(Modifier.width(2.dp))
            OreText(count.toString(), color = if (selected) colors.accent else colors.faint, style = Bd.caption, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (selected) Box(Modifier.fillMaxWidth().height(2.dp).background(colors.signature))
    }
}

@Composable
private fun StatusStrip(state: StorageState, text: StorageText, width: Int) {
    val colors = Bd.colors
    val limited = state.typeLimit in 1 until Int.MAX_VALUE
    Row(Modifier.width(width.dp).heightIn(min = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        OreText(text.types, color = colors.faint, style = Bd.caption, maxLines = 1)
        Spacer(Modifier.width(3.dp))
        val used = formatExact(state.stored.toLong())
        OreText(if (limited) "$used / ${formatExact(state.typeLimit.toLong())}" else used, color = colors.muted, style = Bd.caption, maxLines = 1)
        if (state.shown != state.stored) {
            Spacer(Modifier.width(6.dp))
            OreText(text.filtered, color = colors.faint, style = Bd.caption, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            OreText(formatExact(state.shown.toLong()), color = colors.accentDeep, style = Bd.caption, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        if (limited) BdMeter(state.stored.toFloat() / state.typeLimit, Modifier.width(64.dp).height(7.dp))
    }
}

/** 合成区：3×3 合成格、产物与退回按钮 */
@Composable
private fun CraftSection(craft: CraftState, send: (StorageAction) -> Unit, slots: ComposeMenuSlots<DimensionsNetMenu>, layout: StorageLayout, width: Int) {
    val colors = Bd.colors
    val text = layout.text
    Column(Modifier.width(width.dp)) {
        BdSectionLabel(text.crafting) {
            BdGlyphButton(BdGlyphs.ToStorage, text.clearToStorage, { send(StorageAction.ClearCraft(true)) }, size = 11.dp, glyphSize = 8.dp)
            Spacer(Modifier.width(2.dp))
            BdGlyphButton(BdGlyphs.ToInventory, text.clearToInventory, { send(StorageAction.ClearCraft(false)) }, size = 11.dp, glyphSize = 8.dp)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(colors.line).padding(0.5.dp)) { BdSlotGrid(slots, layout.craftSlots, 3) }
            Spacer(Modifier.width(7.dp))
            OreIcon(OreGlyph.ArrowRight, Modifier.size(8.dp), color = colors.accent)
            Spacer(Modifier.width(7.dp))
            // 产物槽带一圈强调色细框
            Box(Modifier.border(1.dp, colors.accent).padding(2.dp)) { BdSlot(slots, layout.resultSlot) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                OreText(text.returnTitle, color = colors.faint, style = Bd.caption, maxLines = 1)
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
            OreText(text.recipe, color = colors.faint, style = Bd.caption, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            OreText(choices.size.toString(), color = colors.accentDeep, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            OreIcon(OreGlyph.ChevronRight, Modifier.size(6.dp), color = colors.muted)
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
                OptionRow(text.sortNames.getValue(policy), state.secondarySort == policy, enabled = policy != state.sort) {
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
        BdStepper(state.preferredColumns, { send(StorageAction.PreferredSize(it, state.preferredRows)) }, 9..99, shown)
    }
    BdSettingRow(text.rows) {
        BdStepper(state.preferredRows, { send(StorageAction.PreferredSize(state.preferredColumns, it)) }, 2..99, shown)
    }
    BdSectionLabel(text.search)
    BdSettingRow(text.keepSearch) { BdToggle(state.keepSearch, { send(StorageAction.KeepSearch(it)) }, shown) }
    BdSettingRow(text.syncSearch, text.syncSearchHint) { BdToggle(state.syncSearch, { send(StorageAction.SyncSearch(it)) }, shown) }
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
            .background(if (hovered && enabled && !selected) colors.accentSoft else Color.Transparent)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(3.dp).background(if (selected) colors.accent else Color.Transparent))
        Spacer(Modifier.width(4.dp))
        OreText(
            label,
            color =
                when {
                    !enabled -> colors.faint
                    selected -> colors.accentDeep
                    else -> colors.text
                },
            maxLines = 1,
        )
    }
}
