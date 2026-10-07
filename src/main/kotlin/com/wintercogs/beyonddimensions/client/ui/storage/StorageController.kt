package com.wintercogs.beyonddimensions.client.ui.storage

import com.wintercogs.beyonddimensions.Config
import com.wintercogs.beyonddimensions.api.ButtonState
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.client.gui.NetMenuType
import com.wintercogs.beyonddimensions.client.ui.base.resourceIcon
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.common.menu.interaction.ResourceContents
import com.wintercogs.beyonddimensions.common.menu.widget.ClientNetStorage
import com.wintercogs.beyonddimensions.common.menu.widget.ClientNetStorageSearchHelper
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime
import com.wintercogs.beyonddimensions.integration.ModPresence
import com.wintercogs.beyonddimensions.integration.OtherModIds
import com.wintercogs.beyonddimensions.integration.RecipeViewerSearch
import com.wintercogs.beyonddimensions.integration.module.polymorph.ComposeRecipeChoices
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenPrimaryNetSwitcherPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import dev.compixel.forge.item.ItemIcon
import net.minecraft.Util
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.neoforge.network.PacketDistributor

/** 一个分类标签在界面上的样子：名称与图标 */
data class CategoryTab(val name: String, val icon: ItemIcon?)

/** 分类编辑区里的一个特定补充 */
data class CategoryKey(val icon: ItemIcon, val name: String)

/**
 * 分类编辑区：正在编辑第 [index] 个分类。[inspected] 是列出写法的特定补充（没有选中时为图标），
 * [chips] 是它的模组、标签、耐久与额外组件写法，点击加入搜索式
 */
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

data class RecipeChoice(val id: String, val label: String)

/** 合成区的状态；只有合成终端才有 */
data class CraftState(
    /** 能否在存储与合成之间切换（方块终端始终带合成区） */
    val toggleable: Boolean,
    /** 关闭界面时合成格剩余物品优先退回存储 */
    val returnToStorage: Boolean,
    /** 多个配方可选时由 Polymorph 提供的候选 */
    val recipeChoices: List<RecipeChoice>,
)

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
    val sort: ButtonState = ButtonState.SORT_NAME,
    val secondarySort: ButtonState? = null,
    val reverse: Boolean = false,
    val keepSearch: Boolean = false,
    val syncSearch: Boolean = false,
    /** 关闭合成菜单后保留合成格里的物品 */
    val keepCrafting: Boolean = false,
    val craft: CraftState? = null,
)

sealed interface StorageAction {
    data class Search(val text: String) : StorageAction

    data class Scroll(val rows: Int) : StorageAction

    data class ScrollTo(val row: Int) : StorageAction

    /** 界面按可用空间算出的实际行列 */
    data class Viewport(val columns: Int, val rows: Int) : StorageAction

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

    /**
     * 点击第 [index] 个特定补充：左键列出它的写法，右键移除。点末尾的空格时，左键加入手上的物品，右键加入手上容器的内容物
     */
    data class CategoryKeyClick(val index: Int, val right: Boolean) : StorageAction

    /** 把一个写法加入搜索式：按住 Shift 时与已有条件同时符合，否则符合其一即可 */
    data class AppendToSearch(val text: String) : StorageAction

    /** 以下是存储格子右键菜单里的操作 */
    data class TakeFromMenu(val amount: Int) : StorageAction

    data class AddToCategory(val index: Int) : StorageAction

    data object CloseSlotMenu : StorageAction

    data class Sort(val policy: ButtonState) : StorageAction

    data class SecondarySort(val policy: ButtonState?) : StorageAction

    data class Reverse(val enabled: Boolean) : StorageAction

    data class KeepSearch(val enabled: Boolean) : StorageAction

    data class SyncSearch(val enabled: Boolean) : StorageAction

    data object ToggleCraft : StorageAction

    data object OpenSwitcher : StorageAction

    data class ClearCraft(val toStorage: Boolean) : StorageAction

    data class ReturnPreference(val toStorage: Boolean) : StorageAction

    data class KeepCrafting(val enabled: Boolean) : StorageAction

    data class ChooseRecipe(val id: String) : StorageAction
}

/** 可选的排序方式，按界面中的顺序排列 */
val SORT_POLICIES =
    listOf(
        ButtonState.SORT_NAME,
        ButtonState.SORT_QUANTITY,
        ButtonState.SORT_MODID,
        ButtonState.SORT_CREATIVE_TAB,
        ButtonState.SORT_MAX_STACK,
        ButtonState.SORT_INSERTED_TIME,
        ButtonState.SORT_MODIFIED_TIME,
    )

/** 分类停止改动多久后写进文件 */
private const val SAVE_DELAY_MILLIS = 1000L

/**
 * 存储终端（含合成终端）的控制器，运行在游戏线程。
 */
class StorageController(private val menu: DimensionsNetMenu) {
    private val craftMenu = menu as? DimensionsCraftMenu
    private val recipeChoices =
        if (craftMenu != null && ModPresence.isLoaded(OtherModIds.POLYMORPH)) ComposeRecipeChoices() else null
    private var lastSearch = CommonConfigRuntime.uiSearch
    private val player = menu.player.uuid
    private val registries = menu.player.registryAccess()
    private var categories = StorageCategories.load(player, registries)
    private var selected =
        StorageCategories.selected[player]?.takeIf { it in categories.indices } ?: if (categories.isEmpty()) -1 else 0
    private var editing = -1
    private var inspected = -1

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
        restoreTransferContext()
        sendCraftPreference()
    }

    fun snapshot(): StorageState {
        if (CommonConfigRuntime.searchTextWithJEIEMI) followViewerSearch()
        val craft =
            craftMenu?.let {
                CraftState(
                    toggleable = menu !is DimensionsCraftMenuTerminal,
                    returnToStorage = CommonConfigRuntime.uiCraftReturnButton == ButtonState.ENABLED,
                    recipeChoices = recipeChoices?.snapshot(it)
                        ?.map { choice -> RecipeChoice(choice.id(), choice.label()) }.orEmpty(),
                )
            }
        val stored = menu.storage.storage.size
        return StorageState(
            networkName = menu.networkName().string,
            networkId = menu.networkId(),
            columns = menu.columns,
            rows = menu.lines,
            preferredColumns = CommonConfigRuntime.uiColumns,
            preferredRows = CommonConfigRuntime.uiPageNum,
            firstRow = menu.lineData,
            totalRows = menu.maxLineData + menu.lines,
            stored = stored,
            categories = categories.map { CategoryTab(it.name, it.icon?.let(::iconOf)) },
            category = selected,
            editor = editorState(stored),
            slotMenu = slotMenuState(),
            search = lastSearch,
            sort = CommonConfigRuntime.uiSortButton,
            secondarySort = CommonConfigRuntime.uiSecondSortButton.takeIf { it in SORT_POLICIES },
            reverse = CommonConfigRuntime.uiReverseButton == ButtonState.ENABLED,
            keepSearch = CommonConfigRuntime.uiSearchButton == ButtonState.ENABLED,
            syncSearch = CommonConfigRuntime.searchTextWithJEIEMI,
            keepCrafting = CommonConfigRuntime.uiCraftKeep,
            craft = craft,
        )
    }

    fun handle(action: StorageAction) {
        when (action) {
            is StorageAction.Search -> {
                search(action.text)
                if (CommonConfigRuntime.searchTextWithJEIEMI) RecipeViewerSearch.set(action.text)
            }

            is StorageAction.Scroll -> scrollTo(menu.lineData + action.rows)
            is StorageAction.ScrollTo -> scrollTo(action.row)
            is StorageAction.Viewport -> menu.setViewport(action.columns, action.rows)
            is StorageAction.PreferredSize -> {
                CommonConfigRuntime.uiColumns = action.columns
                CommonConfigRuntime.uiPageNum = action.rows
                Config.INSTANCE.commonConfig.UI_COLUMNS.set(action.columns)
                Config.INSTANCE.commonConfig.UI_PAGE_NUM.set(action.rows)
                Config.INSTANCE.commonConfig.UI_PAGE_NUM.save()
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
                CommonConfigRuntime.uiSortButton = action.policy
                Config.INSTANCE.commonConfig.UI_SORT_BUTTON.set(action.policy)
                Config.INSTANCE.commonConfig.UI_SORT_BUTTON.save()
                menu.buildIndexList()
            }

            is StorageAction.SecondarySort -> {
                val policy = action.policy ?: ButtonState.DISABLED
                CommonConfigRuntime.uiSecondSortButton = policy
                Config.INSTANCE.commonConfig.UI_SECOND_SORT_BUTTON.set(policy)
                Config.INSTANCE.commonConfig.UI_SECOND_SORT_BUTTON.save()
                menu.buildIndexList()
            }

            is StorageAction.Reverse -> {
                val state = if (action.enabled) ButtonState.ENABLED else ButtonState.DISABLED
                CommonConfigRuntime.uiReverseButton = state
                Config.INSTANCE.commonConfig.UI_REVERSE_BUTTON.set(state)
                Config.INSTANCE.commonConfig.UI_REVERSE_BUTTON.save()
                menu.buildIndexList()
            }

            is StorageAction.KeepSearch -> {
                val state = if (action.enabled) ButtonState.ENABLED else ButtonState.DISABLED
                CommonConfigRuntime.uiSearchButton = state
                Config.INSTANCE.commonConfig.UI_SEARCH_BUTTON.set(state)
                Config.INSTANCE.commonConfig.UI_SEARCH_BUTTON.save()
            }

            is StorageAction.SyncSearch -> setSearchSync(action.enabled)
            StorageAction.ToggleCraft -> toggleCraft()
            StorageAction.OpenSwitcher -> {
                saveTransferContext()
                PacketDistributor.sendToServer(OpenPrimaryNetSwitcherPacket())
            }

            is StorageAction.ClearCraft -> menu.commands().returnCrafting(action.toStorage)
            is StorageAction.ReturnPreference -> {
                val state = if (action.toStorage) ButtonState.ENABLED else ButtonState.DISABLED
                CommonConfigRuntime.uiCraftReturnButton = state
                Config.INSTANCE.commonConfig.UI_CRAFT_RETURN_BUTTON.set(state)
                Config.INSTANCE.commonConfig.UI_CRAFT_RETURN_BUTTON.save()
                sendCraftPreference()
            }

            is StorageAction.KeepCrafting -> {
                CommonConfigRuntime.uiCraftKeep = action.enabled
                Config.INSTANCE.commonConfig.UI_CRAFT_KEEP.set(action.enabled)
                Config.INSTANCE.commonConfig.UI_CRAFT_KEEP.save()
                sendCraftPreference()
            }

            is StorageAction.ChooseRecipe -> craftMenu?.let { recipeChoices?.select(it, action.id) }
        }
    }

    /** 合成格的偏好由服务器保存在玩家身上，关闭菜单时按它保留或退回；只有合成菜单接收 */
    private fun sendCraftPreference() {
        if (craftMenu == null) return
        menu.commands().preference(
            CommonConfigRuntime.uiCraftReturnButton == ButtonState.ENABLED,
            CommonConfigRuntime.uiCraftKeep,
        )
    }

    /** Shift+Z 切换与配方查看器同步搜索 */
    fun toggleSearchSync() = setSearchSync(!CommonConfigRuntime.searchTextWithJEIEMI)

    private fun setSearchSync(enabled: Boolean) {
        CommonConfigRuntime.searchTextWithJEIEMI = enabled
        Config.INSTANCE.commonConfig.SEARCH_TEXT_WITH_JEI_EMI.set(enabled)
        Config.INSTANCE.commonConfig.SEARCH_TEXT_WITH_JEI_EMI.save()
        if (enabled) RecipeViewerSearch.set(lastSearch)
    }

    private fun search(text: String) {
        if (text == lastSearch) return
        lastSearch = text
        CommonConfigRuntime.uiSearch = text
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

    /** 存储格子的右键菜单：空手右键有资源的格子时打开，返回是否打开了 */
    fun openSlotMenu(slotId: Int): Boolean {
        val resource = (menu.slots.getOrNull(slotId) as? DisorderedStackTypedSlot)?.stack ?: return false
        if (resource.key().isEmpty) return false
        menuTarget = MenuTarget(slotId, resource.key(), resource.amount())
        menuSerial++
        return true
    }

    private fun slotMenuState(): SlotMenu? {
        val target = menuTarget ?: return null
        val key = target.key
        // 数量以打开菜单时为准；服务器按实际库存取出，取不足时只取到现有的数量
        val takeable =
            if (key is ItemStackKey) minOf(target.amount, key.copyStack().maxStackSize.toLong()).toInt() else 0
        return SlotMenu(menuSerial, nameOf(key), takeable, categories.map { it.name })
    }

    /**
     * 编辑区：编辑的分类与当前打开的分类无关，可以一边看着别的分类一边写规则。
     * 符合的种数在分类或存储种数变化时重算，否则每秒最多一次；快照除了每刻一次，输入事件处理完动作后也会再取
     */
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

    /** 一种资源能写进搜索式的条件：模组、ID、各个标签，以及耐久度和额外组件 */
    private fun chipsOf(key: IStackKey<*>): List<String> {
        val chips = ArrayList<String>()
        chips += "@" + key.modId.lowercase()
        if (key is ItemStackKey) chips += "*" + BuiltInRegistries.ITEM.getKey(key.source).path
        key.tags.map { "#" + it.location() }.sorted().forEach { chips += it }
        val stack = (key as? ItemStackKey)?.readOnlyStack
        if (stack != null && stack.isDamageableItem) chips += "%"
        val patch =
            when (key) {
                is ItemStackKey -> key.readOnlyStack.componentsPatch
                is FluidStackKey -> key.readOnlyStack.componentsPatch
                else -> null
            }
        if (patch != null && !patch.isEmpty) chips += "&"
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

    /** 菜单关闭或分类停止改动一会儿后再写文件：输入名称或搜索式时不必每个字都写一次，游戏线程也不会因写盘卡顿 */
    fun tick() {
        if (unsavedSince != 0L && Util.getMillis() - unsavedSince >= SAVE_DELAY_MILLIS) saveNow()
    }

    /** 把还没写进文件的分类立刻写入；菜单关闭时调用 */
    fun saveNow() {
        if (unsavedSince == 0L) return
        unsavedSince = 0L
        StorageCategories.save(player, registries)
    }

    private fun save() {
        version++
        StorageCategories.update(player, categories)
        unsavedSince = Util.getMillis()
        // 编辑搜索式时每次输入都会解析一条，只留下仍在使用的
        val searches = categories.mapTo(HashSet()) { it.search }
        queries.keys.retainAll(searches)
    }

    /** 在存储与合成之间切换会重新打开菜单，先记下滚动位置 */
    private fun toggleCraft() {
        val toCraft = craftMenu == null
        val state = if (toCraft) ButtonState.ENABLED else ButtonState.DISABLED
        CommonConfigRuntime.uiCraftButton = state
        Config.INSTANCE.commonConfig.UI_CRAFT_BUTTON.set(state)
        Config.INSTANCE.commonConfig.UI_CRAFT_BUTTON.save()
        saveTransferContext()
        val target = if (toCraft) NetMenuType.NET_CRAFT_MENU else NetMenuType.NET_MENU
        PacketDistributor.sendToServer(OpenNetGuiPacket(menu.player.stringUUID, target))
    }

    // 指针不用处理：服务器直接换菜单（SwitchMenuProvider），客户端不会收起指针
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
        val keep = CommonConfigRuntime.uiSearchButton == ButtonState.ENABLED && lastSearch.isNotEmpty()
        val saved = if (keep) lastSearch else ""
        CommonConfigRuntime.uiSearch = saved
        Config.INSTANCE.commonConfig.UI_SEARCH.set(saved)
        Config.INSTANCE.commonConfig.UI_SEARCH.save()
    }
}
