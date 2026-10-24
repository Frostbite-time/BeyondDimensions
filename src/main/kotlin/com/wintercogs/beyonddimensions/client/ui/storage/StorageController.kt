package com.wintercogs.beyonddimensions.client.ui.storage

import com.wintercogs.beyonddimensions.Config
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.client.ui.base.resourceIcon
import com.wintercogs.beyonddimensions.common.init.BDPackets
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.common.menu.interaction.ResourceContents
import com.wintercogs.beyonddimensions.common.menu.widget.ClientNetStorage
import com.wintercogs.beyonddimensions.common.menu.widget.ClientNetStorageSearchHelper
import com.wintercogs.beyonddimensions.common.menu.widget.StorageSort
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import com.wintercogs.beyonddimensions.integration.RecipeViewerSearch
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenPrimaryNetSwitcherPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import dev.compixel.forge.item.ItemIcon
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n

/** 一个分类标签在界面上的样子：名称与图标 */
data class CategoryTab(val name: String, val icon: ItemIcon?)

/** 分类编辑区里的一个特定补充 */
data class CategoryKey(val icon: ItemIcon, val name: String)

/** 分类编辑区：正在编辑第 [index] 个分类。[inspected] 是选中的特定补充，[chips] 是它能加入搜索式的写法 */
data class CategoryEditor(
    val index: Int,
    val name: String,
    val icon: ItemIcon?,
    val search: String,
    val matches: Int,
    val keys: List<CategoryKey>,
    val inspected: Int,
    val chips: List<String>,
)

/**
 * 存储格子的右键菜单。[serial] 每次打开都不同；[takeable] 是最多能取出的数量，不是物品时为 0
 */
data class SlotMenu(val serial: Int, val name: String, val takeable: Int, val categories: List<String>)

data class StorageState(
    val networkName: String = "",
    val networkId: Int = -1,
    val columns: Int = 9,
    val rows: Int = 5,
    val preferredColumns: Int = 9,
    val preferredRows: Int = 5,
    val firstRow: Int = 0,
    val totalRows: Int = 0,
    val stored: Int = 0,
    val categories: List<CategoryTab> = emptyList(),
    /** 打开的分类标签，没有时为 -1 */
    val category: Int = -1,
    val editor: CategoryEditor? = null,
    val slotMenu: SlotMenu? = null,
    val search: String = "",
    val sort: StorageSort = StorageSort.NAME,
    val secondarySort: StorageSort? = null,
    val reverse: Boolean = false,
    val keepSearch: Boolean = false,
    val syncSearch: Boolean = false,
    /** 空手右键资源时打开菜单，否则取出一半 */
    val rightClickMenu: Boolean = true,
    /** 关闭界面后保留工作台里的物品 */
    val keepCrafting: Boolean = false,
    /** 工作台被清空时物品优先退回存储，否则优先退回背包 */
    val returnToStorage: Boolean = false,
    /** 工作区显示的工作台 */
    val workbench: String = "",
    /** 工作区展开着 */
    val workbenchShown: Boolean = false,
)

sealed interface StorageAction {
    data class Search(val text: String) : StorageAction

    data class Scroll(val rows: Int) : StorageAction

    data class ScrollTo(val row: Int) : StorageAction

    /** 玩家设定的首选行列，会写入配置 */
    data class PreferredSize(val columns: Int, val rows: Int) : StorageAction

    data class SelectCategory(val index: Int) : StorageAction

    /** 新建分类并打开编辑区 */
    data object AddCategory : StorageAction

    data class EditCategory(val index: Int) : StorageAction

    data object CloseEditor : StorageAction

    data class DeleteCategory(val index: Int) : StorageAction

    data class MoveCategory(val from: Int, val to: Int) : StorageAction

    /** 以下修改编辑区里的分类 */
    data class RenameCategory(val name: String) : StorageAction

    data class CategorySearch(val text: String) : StorageAction

    /** 左键把手上的物品设为图标，右键清除 */
    data class CategoryIcon(val right: Boolean) : StorageAction

    /** 点击第 [index] 个特定补充：左键列出它的写法，右键移除；点末尾的空格是加入手上的物品或容器的内容物 */
    data class CategoryKeyClick(val index: Int, val right: Boolean) : StorageAction

    /** 把一个写法加入搜索式：按住 Shift 时与已有条件同时符合，否则符合其一即可 */
    data class AppendToSearch(val text: String) : StorageAction

    /** 以下是存储格子右键菜单里的操作 */
    data class TakeFromMenu(val amount: Int) : StorageAction

    data class AddToCategory(val index: Int) : StorageAction

    data object CloseSlotMenu : StorageAction

    data class Sort(val policy: StorageSort) : StorageAction

    data class SecondarySort(val policy: StorageSort?) : StorageAction

    data class Reverse(val enabled: Boolean) : StorageAction

    data class KeepSearch(val enabled: Boolean) : StorageAction

    data class SyncSearch(val enabled: Boolean) : StorageAction

    data class RightClickMenu(val enabled: Boolean) : StorageAction

    data object OpenSwitcher : StorageAction

    data class KeepCrafting(val enabled: Boolean) : StorageAction

    /** 设定工作台被清空时物品的退回方向，所有工作台共用，会写入配置 */
    data class ReturnToStorage(val enabled: Boolean) : StorageAction

    /** 展开或收起工作区，会写入配置 */
    data class ShowWorkbench(val shown: Boolean) : StorageAction

    /** 选择工作区显示的工作台，会写入配置 */
    data class SelectWorkbench(val id: String) : StorageAction
}

/** 分类停止改动多久后写进文件 */
private const val SAVE_DELAY_MILLIS = 1000L

/**
 * 存储界面的控制器，运行在游戏线程。
 */
class StorageController(private val menu: DimensionsNetMenu, private val workbenches: StorageWorkbenchHost) {
    private var lastSearch = ClientConfigRuntime.storageSearch
    private val player = menu.player.uuid
    private var categories = StorageCategories.load(player)
    private var selected =
        StorageCategories.selected[player]?.takeIf { it in categories.indices } ?: if (categories.isEmpty()) -1 else 0
    private var editing = -1
    private var inspected = -1

    // 格子的行列按工作区展开与否定下；配方查看器在别处展开工作区时据此重新排
    private var fittedShown = false

    // 同一条搜索式始终用同一个查询对象，分类的匹配结果才能缓存
    private val queries = HashMap<String, ClientNetStorageSearchHelper.Query>()
    private val icons = HashMap<IStackKey<*>, ItemIcon>()
    private var matches = 0
    private var countedEditing = -1
    private var countedVersion = -1
    private var countedStored = -1
    private var countedAt = 0L

    // 分类每改动一次加一，编辑区里符合的种数随之重算
    private var version = 0
    private var menuTarget: MenuTarget? = null
    private var menuSerial = 0

    // 最后一次改动分类而还没写进文件的时间，0 表示都已写入
    private var unsavedSince = 0L

    /** 右键菜单针对的资源 */
    private class MenuTarget(val slot: Int, val key: IStackKey<*>, val amount: Long)

    init {
        menu.loadSearchText(lastSearch)
        menu.setCategory(filterOf(categories.getOrNull(selected)))
        // 记下的滚动行号按实际列数计算，先定好列数再恢复
        fitViewport()
        restoreTransferContext()
        sendWorkbenchPreference(menu)
    }

    fun snapshot(): StorageState {
        if (ClientConfigRuntime.recipeViewerSyncSearch) followViewerSearch()
        if (workbenchShown() != fittedShown) fitViewport()
        val stored = menu.storage.storage.size
        return StorageState(
            networkName = menu.networkName().string,
            networkId = menu.networkId(),
            columns = menu.columns,
            rows = menu.lines,
            preferredColumns = ClientConfigRuntime.storageColumns,
            preferredRows = ClientConfigRuntime.storageRows,
            firstRow = menu.lineData,
            totalRows = menu.maxLineData + menu.lines,
            stored = stored,
            categories = categories.map { CategoryTab(it.name, it.icon?.let(::iconOf)) },
            category = selected,
            editor = editorState(stored),
            slotMenu = slotMenuState(),
            search = lastSearch,
            sort = ClientConfigRuntime.storageSort,
            secondarySort = ClientConfigRuntime.storageSecondarySort.takeIf { ClientConfigRuntime.storageUseSecondarySort },
            reverse = ClientConfigRuntime.storageReverse,
            keepSearch = ClientConfigRuntime.storageKeepSearch,
            syncSearch = ClientConfigRuntime.recipeViewerSyncSearch,
            rightClickMenu = ClientConfigRuntime.storageRightClickMenu,
            keepCrafting = ClientConfigRuntime.craftingKeepGrid,
            returnToStorage = ClientConfigRuntime.craftingReturnToStorage,
            workbench = workbenches.resolve(ClientConfigRuntime.storageWorkbench),
            workbenchShown = fittedShown,
        )
    }

    fun handle(action: StorageAction) {
        when (action) {
            is StorageAction.Search -> {
                search(action.text)
                if (ClientConfigRuntime.recipeViewerSyncSearch) RecipeViewerSearch.set(action.text)
            }

            is StorageAction.Scroll -> scrollTo(menu.lineData + action.rows)
            is StorageAction.ScrollTo -> scrollTo(action.row)
            is StorageAction.PreferredSize -> {
                ClientConfigRuntime.storageColumns = action.columns
                ClientConfigRuntime.storageRows = action.rows
                Config.INSTANCE.clientConfig.STORAGE_COLUMNS.set(action.columns)
                Config.INSTANCE.clientConfig.STORAGE_ROWS.set(action.rows)
                Config.INSTANCE.clientConfig.STORAGE_ROWS.save()
            }

            is StorageAction.SelectCategory -> select(action.index)
            StorageAction.AddCategory -> {
                categories = categories + StorageCategory(I18n.get("ui.beyonddimensions.storage.category.new"))
                save()
                openEditor(categories.lastIndex)
            }

            is StorageAction.EditCategory -> openEditor(action.index)

            StorageAction.CloseEditor -> {
                editing = -1
                inspected = -1
            }

            is StorageAction.DeleteCategory -> delete(action.index)
            is StorageAction.MoveCategory -> move(action.from, action.to)
            is StorageAction.RenameCategory -> edit { it.copy(name = action.name) }
            is StorageAction.CategorySearch -> edit { it.copy(search = action.text) }
            is StorageAction.CategoryIcon -> {
                val carried = menu.carried
                if (action.right) edit { it.copy(icon = null) }
                else if (!carried.isEmpty) edit { it.copy(icon = ItemStackKey(carried)) }
            }

            is StorageAction.CategoryKeyClick -> clickKey(action.index, action.right)
            is StorageAction.AppendToSearch -> edit { category ->
                val search = category.search.trimEnd()
                category.copy(
                    search = when {
                        search.isEmpty() -> action.text
                        Screen.hasShiftDown() -> "$search ${action.text}"
                        else -> "$search|${action.text}"
                    }
                )
            }

            is StorageAction.TakeFromMenu -> {
                menuTarget?.let { menu.commands().take(it.slot, it.key, action.amount.toLong()) }
                menuTarget = null
            }

            is StorageAction.AddToCategory -> {
                val key = menuTarget?.key
                menuTarget = null
                if (key != null && action.index in categories.indices) {
                    update(action.index) { if (key in it.keys) it else it.copy(keys = it.keys + key) }
                }
            }

            StorageAction.CloseSlotMenu -> menuTarget = null

            is StorageAction.Sort -> {
                ClientConfigRuntime.storageSort = action.policy
                Config.INSTANCE.clientConfig.STORAGE_SORT.set(action.policy)
                Config.INSTANCE.clientConfig.STORAGE_SORT.save()
                menu.buildIndexList()
            }

            is StorageAction.SecondarySort -> {
                // 选"无"时只关掉开关，原来选的次排序留在配置里
                val policy = action.policy
                ClientConfigRuntime.storageUseSecondarySort = policy != null
                Config.INSTANCE.clientConfig.STORAGE_USE_SECONDARY_SORT.set(policy != null)
                if (policy != null) {
                    ClientConfigRuntime.storageSecondarySort = policy
                    Config.INSTANCE.clientConfig.STORAGE_SECONDARY_SORT.set(policy)
                }
                Config.INSTANCE.clientConfig.STORAGE_SECONDARY_SORT.save()
                menu.buildIndexList()
            }

            is StorageAction.Reverse -> {
                ClientConfigRuntime.storageReverse = action.enabled
                Config.INSTANCE.clientConfig.STORAGE_REVERSE.set(action.enabled)
                Config.INSTANCE.clientConfig.STORAGE_REVERSE.save()
                menu.buildIndexList()
            }

            is StorageAction.KeepSearch -> {
                ClientConfigRuntime.storageKeepSearch = action.enabled
                Config.INSTANCE.clientConfig.STORAGE_KEEP_SEARCH.set(action.enabled)
                Config.INSTANCE.clientConfig.STORAGE_KEEP_SEARCH.save()
            }

            is StorageAction.SyncSearch -> setSearchSync(action.enabled)
            is StorageAction.RightClickMenu -> {
                ClientConfigRuntime.storageRightClickMenu = action.enabled
                Config.INSTANCE.clientConfig.STORAGE_RIGHT_CLICK_MENU.set(action.enabled)
                Config.INSTANCE.clientConfig.STORAGE_RIGHT_CLICK_MENU.save()
            }

            StorageAction.OpenSwitcher -> {
                saveTransferContext()
                BDPackets.INSTANCE.sendToServer(OpenPrimaryNetSwitcherPacket())
            }

            is StorageAction.KeepCrafting -> {
                ClientConfigRuntime.craftingKeepGrid = action.enabled
                Config.INSTANCE.clientConfig.CRAFTING_KEEP_GRID.set(action.enabled)
                Config.INSTANCE.clientConfig.CRAFTING_KEEP_GRID.save()
                sendWorkbenchPreference(menu)
            }

            is StorageAction.ReturnToStorage -> {
                ClientConfigRuntime.craftingReturnToStorage = action.enabled
                Config.INSTANCE.clientConfig.CRAFTING_RETURN_TO_STORAGE.set(action.enabled)
                Config.INSTANCE.clientConfig.CRAFTING_RETURN_TO_STORAGE.save()
                sendWorkbenchPreference(menu)
            }

            is StorageAction.ShowWorkbench -> StorageWorkbenchChoice.show(action.shown)
            is StorageAction.SelectWorkbench -> StorageWorkbenchChoice.select(action.id)
        }
        // 首选行列、编辑区与工作区都影响能放下的格数
        fitViewport()
    }

    /** 按屏幕尺寸与工作区定下格子的实际行列，设到菜单上 */
    fun fitViewport() {
        val window = Minecraft.getInstance().window
        // 与界面拿到的尺寸相同：1 dp 是一个界面像素
        val scale = window.guiScale.toFloat()
        val shown = workbenchShown()
        fittedShown = shown
        val (columns, rows) = fittedGridSize(
            screenWidth = (window.width / scale).toInt(),
            screenHeight = (window.height / scale).toInt(),
            editorOpen = editing in categories.indices,
            preferredColumns = ClientConfigRuntime.storageColumns,
            preferredRows = ClientConfigRuntime.storageRows,
            bottomHeight = { width -> bottomHeight(width, workbenches, shown) },
        )
        menu.setViewport(columns, rows)
    }

    // 没有能用的工作台时工作区无从展开
    private fun workbenchShown() = ClientConfigRuntime.storageWorkbenchShown && !workbenches.isEmpty

    private fun setSearchSync(enabled: Boolean) {
        ClientConfigRuntime.recipeViewerSyncSearch = enabled
        Config.INSTANCE.clientConfig.RECIPE_VIEWER_SYNC_SEARCH.set(enabled)
        Config.INSTANCE.clientConfig.RECIPE_VIEWER_SYNC_SEARCH.save()
        if (enabled) RecipeViewerSearch.set(lastSearch)
    }

    private fun search(text: String) {
        if (text == lastSearch) return
        lastSearch = text
        ClientConfigRuntime.storageSearch = text
        menu.loadSearchText(text)
        menu.lineData = 0
        menu.markForceAllUpdateClientView()
        menu.updateViewerStorage(false)
    }

    /** 配方查看器的搜索框被修改时，同步到本界面 */
    private fun followViewerSearch() {
        val viewer = RecipeViewerSearch.get() ?: return
        if (viewer != lastSearch) search(viewer)
    }

    private fun scrollTo(row: Int) {
        val target = row.coerceIn(0, menu.maxLineData)
        if (target == menu.lineData) return
        menu.lineData = target
        menu.buildIndexList()
    }

    /** 存储格子的右键菜单：配置开启时，空手右键有资源的格子会打开它；返回是否打开了 */
    fun openSlotMenu(slotId: Int): Boolean {
        if (!ClientConfigRuntime.storageRightClickMenu) return false
        val resource = (menu.slots.getOrNull(slotId) as? DisorderedStackTypedSlot)?.stack ?: return false
        if (resource.key().isEmpty) return false
        menuTarget = MenuTarget(slotId, resource.key(), resource.amount())
        menuSerial++
        return true
    }

    private fun slotMenuState(): SlotMenu? {
        val target = menuTarget ?: return null
        val key = target.key
        // 数量以打开菜单时为准，服务器按实际库存取出
        val takeable =
            if (key is ItemStackKey) minOf(target.amount, key.copyStack().maxStackSize.toLong()).toInt() else 0
        return SlotMenu(menuSerial, nameOf(key), takeable, categories.map { it.name })
    }

    /** 编辑区：编辑的分类与当前打开的分类无关。符合的种数在分类或存储种数变化时重算，否则每秒最多一次 */
    private fun editorState(stored: Int): CategoryEditor? {
        val category = categories.getOrNull(editing) ?: return null
        val now = Util.getMillis()
        val changed = countedEditing != editing || countedVersion != version || countedStored != stored
        if (changed || now - countedAt >= 1000) {
            countedEditing = editing
            countedVersion = version
            countedStored = stored
            countedAt = now
            matches = menu.clientNetStorage?.count(filterOf(category)) ?: 0
        }
        val inspectedKey = category.keys.getOrNull(inspected) ?: category.icon
        return CategoryEditor(
            index = editing,
            name = category.name,
            icon = category.icon?.let(::iconOf),
            search = category.search,
            matches = matches,
            keys = category.keys.map { CategoryKey(iconOf(it), nameOf(it)) },
            inspected = if (inspected in category.keys.indices) inspected else -1,
            chips = inspectedKey?.let(::chipsOf).orEmpty(),
        )
    }

    /** 一种资源能写进搜索式的条件：模组、资源类型、ID、各个标签，以及耐久度和额外组件 */
    private fun chipsOf(key: IStackKey<*>): List<String> {
        val chips = ArrayList<String>()
        chips += "@" + key.modId.lowercase()
        chips += "~" + key.typeId.path.substringAfterLast('/')
        // 能量这类资源的 ID 就是类型 ID，不再重复列出
        if (key.sourceId != key.typeId) chips += "*" + key.sourceId.path
        key.tags.map { "#" + it.location() }.sorted().forEach { chips += it }
        val stack = (key as? ItemStackKey)?.readOnlyStack
        if (stack != null && stack.isDamageableItem) chips += "%"
        if (ClientNetStorageSearchHelper.extraDataKeys(key).isNotEmpty()) chips += "&"
        return chips
    }

    // 物品的显示名带方括号，界面上用不带括号的名称
    private fun nameOf(key: IStackKey<*>): String =
        if (key is ItemStackKey) key.readOnlyStack.hoverName.string else key.render.getDisplayName(key).string

    private fun iconOf(key: IStackKey<*>) = icons.getOrPut(key) { resourceIcon(key) }

    private fun filterOf(category: StorageCategory?): ClientNetStorage.CategoryFilter {
        if (category == null) return ClientNetStorage.CategoryFilter.ALL
        val query = queries.getOrPut(category.search) { ClientNetStorageSearchHelper.Query.parse(category.search) }
        return ClientNetStorage.CategoryFilter(query, category.keys.toSet())
    }

    private fun select(index: Int) {
        val target = if (index in categories.indices) index else -1
        // 选中已打开的分类时不重新应用，滚动位置保持不变
        if (target == selected) return
        selected = target
        if (target >= 0) StorageCategories.selected[player] = target else StorageCategories.selected.remove(player)
        applyCategory()
    }

    private fun applyCategory() {
        val filter = filterOf(categories.getOrNull(selected))
        menu.setCategory(filter)
        menu.lineData = 0
        menu.updateViewerStorage(false)
    }

    /** 在编辑区打开第 [index] 个分类，不切换当前打开的分类 */
    private fun openEditor(index: Int) {
        if (index !in categories.indices) return
        if (index != editing) inspected = -1
        editing = index
    }

    /** 修改编辑区里的分类 */
    private fun edit(change: (StorageCategory) -> StorageCategory) {
        if (editing in categories.indices) update(editing, change)
    }

    private fun update(index: Int, change: (StorageCategory) -> StorageCategory) {
        val before = categories[index]
        val after = change(before)
        if (after == before) return
        categories = categories.toMutableList().also { it[index] = after }
        save()
        if (index == selected && (after.search != before.search || after.keys != before.keys)) applyCategory()
    }

    private fun clickKey(index: Int, right: Boolean) {
        val category = categories.getOrNull(editing) ?: return
        if (index in category.keys.indices) {
            if (!right) {
                inspected = if (inspected == index) -1 else index
                return
            }
            if (inspected == index) inspected = -1 else if (inspected > index) inspected--
            edit { it.copy(keys = it.keys.filterIndexed { i, _ -> i != index }) }
            return
        }
        val carried = menu.carried
        if (carried.isEmpty) return
        val key = if (right) ResourceContents.of(carried) else ItemStackKey(carried.copyWithCount(1))
        if (key == null || key in category.keys) return
        edit { it.copy(keys = it.keys + key) }
    }

    private fun delete(index: Int) {
        if (index !in categories.indices) return
        categories = categories.filterIndexed { i, _ -> i != index }
        save()
        if (editing == index) inspected = -1
        editing =
            when {
                editing == index -> -1
                editing > index -> editing - 1
                else -> editing
            }
        selected =
            when {
                selected == index -> minOf(index, categories.lastIndex)
                selected > index -> selected - 1
                else -> selected
            }
        if (selected >= 0) StorageCategories.selected[player] = selected else StorageCategories.selected.remove(player)
        applyCategory()
    }

    private fun move(from: Int, to: Int) {
        if (from !in categories.indices || to !in categories.indices || from == to) return
        val list = categories.toMutableList()
        list.add(to, list.removeAt(from))
        categories = list
        save()
        fun moved(index: Int) =
            when {
                index == from -> to
                from < to && index in from + 1..to -> index - 1
                from > to && index in to until from -> index + 1
                else -> index
            }
        selected = moved(selected)
        editing = if (editing >= 0) moved(editing) else -1
        if (selected >= 0) StorageCategories.selected[player] = selected
    }

    /** 菜单关闭或分类停止改动一会儿后再写文件，不必每个字都写一次 */
    fun tick() {
        if (unsavedSince != 0L && Util.getMillis() - unsavedSince >= SAVE_DELAY_MILLIS) saveNow()
    }

    /** 把还没写进文件的分类立刻写入；菜单关闭时调用 */
    fun saveNow() {
        if (unsavedSince == 0L) return
        unsavedSince = 0L
        StorageCategories.save(player)
    }

    private fun save() {
        version++
        StorageCategories.update(player, categories)
        unsavedSince = Util.getMillis()
        // 编辑搜索式时每次输入都会解析一条，只留下仍在使用的
        val searches = categories.mapTo(HashSet()) { it.search }
        queries.keys.retainAll(searches)
    }

    // 服务器直接换菜单（见 SwitchMenuProvider），指针留在原处，只需记下滚动位置
    private fun saveTransferContext() {
        UIDataHelper.currentPage = menu.lineData
        UIDataHelper.isTransfer = true
    }

    private fun restoreTransferContext() {
        if (!UIDataHelper.isTransfer) return
        UIDataHelper.isTransfer = false
        menu.lineData = UIDataHelper.currentPage
    }

    /** 菜单关闭时按偏好保留或清空搜索文字 */
    fun saveSearch() {
        val keep = ClientConfigRuntime.storageKeepSearch && lastSearch.isNotEmpty()
        val saved = if (keep) lastSearch else ""
        ClientConfigRuntime.storageSearch = saved
        Config.INSTANCE.clientConfig.STORAGE_SEARCH.set(saved)
        Config.INSTANCE.clientConfig.STORAGE_SEARCH.save()
    }
}
