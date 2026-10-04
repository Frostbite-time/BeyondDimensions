package com.wintercogs.beyonddimensions.client.ui.storage

import com.wintercogs.beyonddimensions.Config
import com.wintercogs.beyonddimensions.api.ButtonState
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.api.storage.key.impl.LongStackKey
import com.wintercogs.beyonddimensions.client.gui.NetMenuType
import com.wintercogs.beyonddimensions.client.ui.base.BdController
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime
import com.wintercogs.beyonddimensions.integration.ModPresence
import com.wintercogs.beyonddimensions.integration.OtherModIds
import com.wintercogs.beyonddimensions.integration.RecipeViewerSearch
import com.wintercogs.beyonddimensions.integration.module.polymorph.ComposeRecipeChoices
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenPrimaryNetSwitcherPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec2
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW

/** 资源类型页签 */
data class TypeTab(val id: String, val label: String, val count: Int)

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
    val ready: Boolean = false,
    val networkName: String = "",
    val networkId: Int = -1,
    val columns: Int = 9,
    val rows: Int = 5,
    val preferredColumns: Int = 9,
    val preferredRows: Int = 5,
    val firstRow: Int = 0,
    val totalRows: Int = 0,
    val stored: Int = 0,
    val shown: Int = 0,
    val typeLimit: Int = Int.MAX_VALUE,
    val tabs: List<TypeTab> = emptyList(),
    val typeFilter: String? = null,
    val search: String = "",
    val sort: ButtonState = ButtonState.SORT_NAME,
    val secondarySort: ButtonState? = null,
    val reverse: Boolean = false,
    val keepSearch: Boolean = false,
    val syncSearch: Boolean = false,
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

    data class Filter(val typeId: String?) : StorageAction

    data class Sort(val policy: ButtonState) : StorageAction

    data class SecondarySort(val policy: ButtonState?) : StorageAction

    data class Reverse(val enabled: Boolean) : StorageAction

    data class KeepSearch(val enabled: Boolean) : StorageAction

    data class SyncSearch(val enabled: Boolean) : StorageAction

    data object ToggleCraft : StorageAction

    data object OpenSwitcher : StorageAction

    data class ClearCraft(val toStorage: Boolean) : StorageAction

    data class ReturnPreference(val toStorage: Boolean) : StorageAction

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

/**
 * 存储终端（含合成终端）的控制器，运行在游戏线程。
 */
class StorageController(private val menu: DimensionsNetMenu) : BdController<StorageState, StorageAction> {
    private val craftMenu = menu as? DimensionsCraftMenu
    private val recipeChoices =
        if (craftMenu != null && ModPresence.isLoaded(OtherModIds.POLYMORPH)) ComposeRecipeChoices() else null
    private var lastSearch = CommonConfigRuntime.uiSearch
    private var tabs = emptyList<TypeTab>()
    private var tabsStored = -1
    private var tabsCountedAt = 0L

    init {
        menu.loadSearchText(lastSearch)
        restoreTransferContext()
        craftMenu?.let { menu.commands().preference(CommonConfigRuntime.uiCraftReturnButton == ButtonState.ENABLED) }
    }

    override fun snapshot(): StorageState {
        if (CommonConfigRuntime.searchTextWithJEIEMI) followViewerSearch()
        val craft =
            craftMenu?.let {
                CraftState(
                    toggleable = menu !is DimensionsCraftMenuTerminal,
                    returnToStorage = CommonConfigRuntime.uiCraftReturnButton == ButtonState.ENABLED,
                    recipeChoices = recipeChoices?.snapshot(it)?.map { choice -> RecipeChoice(choice.id(), choice.label()) }.orEmpty(),
                )
            }
        val stored = menu.storage.storage.size
        return StorageState(
            ready = menu.ready(),
            networkName = menu.networkName().string,
            networkId = menu.networkId(),
            columns = menu.columns,
            rows = menu.lines,
            preferredColumns = CommonConfigRuntime.uiColumns,
            preferredRows = CommonConfigRuntime.uiPageNum,
            firstRow = menu.lineData,
            totalRows = menu.maxLineData + menu.lines,
            stored = stored,
            shown = menu.clientNetStorage?.storage?.size ?: 0,
            typeLimit = menu.typeLimit(),
            tabs = typeTabs(stored),
            typeFilter = menu.clientNetStorage?.typeFilter?.toString(),
            search = lastSearch,
            sort = CommonConfigRuntime.uiSortButton,
            secondarySort = CommonConfigRuntime.uiSecondSortButton.takeIf { it in SORT_POLICIES },
            reverse = CommonConfigRuntime.uiReverseButton == ButtonState.ENABLED,
            keepSearch = CommonConfigRuntime.uiSearchButton == ButtonState.ENABLED,
            syncSearch = CommonConfigRuntime.searchTextWithJEIEMI,
            craft = craft,
        )
    }

    override fun handle(action: StorageAction) {
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
            is StorageAction.Filter -> {
                menu.setTypeFilter(action.typeId?.let(ResourceLocation::tryParse))
                menu.lineData = 0
                menu.updateViewerStorage(false)
            }
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
                menu.commands().preference(action.toStorage)
            }
            is StorageAction.ChooseRecipe -> craftMenu?.let { recipeChoices?.select(it, action.id) }
        }
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

    /** 各类资源的页签；种类数不变时每秒最多重算一次 */
    private fun typeTabs(stored: Int): List<TypeTab> {
        // 快照除了每刻一次，输入事件处理完动作后也会再取，所以按时间而不是按调用次数限制重算
        val now = Util.getMillis()
        if (stored == tabsStored && now - tabsCountedAt < 1000) return tabs
        tabsStored = stored
        tabsCountedAt = now
        val counts = LinkedHashMap<ResourceLocation, Int>()
        val samples = HashMap<ResourceLocation, IStackKey<*>>()
        for (value in menu.storage.storage) {
            val key = value.key()
            if (key.isEmpty) continue
            counts.merge(key.typeId, 1, Int::plus)
            samples.putIfAbsent(key.typeId, key)
        }
        val order = listOf(ItemStackKey.ID, FluidStackKey.ID)
        val ids = counts.keys.sortedWith(compareBy({ order.indexOf(it).let { index -> if (index < 0) order.size else index } }, { it.toString() }))
        val next = ids.map { TypeTab(it.toString(), typeLabel(it, samples.getValue(it)), counts.getValue(it)) }
        if (next != tabs) tabs = next
        return tabs
    }

    private fun typeLabel(typeId: ResourceLocation, sample: IStackKey<*>): String {
        val key = "ui.beyonddimensions.resource_type.${typeId.namespace}.${typeId.path.replace('/', '.')}"
        if (I18n.exists(key)) return I18n.get(key)
        if (sample is LongStackKey<*>) return sample.render.getDisplayName(sample).string
        return typeId.path.substringAfterLast('/')
    }

    /** 在存储与合成之间切换会重新打开菜单，先记下滚动位置与指针位置 */
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

    private fun saveTransferContext() {
        UIDataHelper.currentPage = menu.lineData
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        GLFW.glfwGetCursorPos(Minecraft.getInstance().window.window, x, y)
        UIDataHelper.lastMousePos = Vec2(x[0].toFloat(), y[0].toFloat())
        UIDataHelper.isTransfer = true
    }

    private fun restoreTransferContext() {
        if (!UIDataHelper.isTransfer) return
        UIDataHelper.isTransfer = false
        menu.lineData = UIDataHelper.currentPage
        UIDataHelper.lastMousePos?.let {
            GLFW.glfwSetCursorPos(Minecraft.getInstance().window.window, it.x.toDouble(), it.y.toDouble())
        }
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
