package com.wintercogs.beyonddimensions.client.ui.storage

import com.wintercogs.beyonddimensions.Config
import com.wintercogs.beyonddimensions.api.ButtonState
import com.wintercogs.beyonddimensions.client.gui.NetMenuType
import com.wintercogs.beyonddimensions.client.ui.base.BdController
import com.wintercogs.beyonddimensions.client.ui.base.BdIcons
import com.wintercogs.beyonddimensions.client.ui.base.tr
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime
import com.wintercogs.beyonddimensions.integration.ModPresence
import com.wintercogs.beyonddimensions.integration.OtherModIds
import com.wintercogs.beyonddimensions.integration.module.jei.BDjeiPlugin
import com.wintercogs.beyonddimensions.network.packet.c2s.ClickTransferCraftButtonPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenPrimaryNetSwitcherPacket
import com.wintercogs.beyonddimensions.util.UIDataHelper
import dev.compixel.forge.item.ItemIcon
import dev.emi.emi.api.EmiApi
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec2
import net.neoforged.neoforge.network.PacketDistributor
import org.lwjgl.glfw.GLFW

/** 侧栏按钮：图标与提示随状态变化 */
data class SideButton(val icon: ItemIcon, val tooltip: String)

/** 合成区的小按钮，使用 Ore 的像素图标 */
data class CraftButton(val icon: ItemIcon, val tooltip: String)

data class StorageState(
    /** 显示的存储行数 */
    val lines: Int,
    /** 当前第一行在全部结果中的行号，与最大可滚动行号 */
    val line: Int,
    val maxLine: Int,
    val search: String,
    /** 由游戏侧改写搜索文字（配方查看器同步）时递增，界面据此覆盖输入框 */
    val searchRevision: Int,
    val buttons: List<Pair<StorageAction, SideButton>>,
    /** 合成区上方的三个小按钮；不是合成界面时为空 */
    val craftButtons: List<Pair<StorageAction, CraftButton>> = emptyList(),
)

sealed interface StorageAction {
    data class Search(val text: String) : StorageAction

    data class ScrollTo(val line: Int) : StorageAction

    data object CycleSort : StorageAction

    data object CycleSecondSort : StorageAction

    data object ToggleReverse : StorageAction

    data object ToggleSearchMode : StorageAction

    data object AddRow : StorageAction

    data object RemoveRow : StorageAction

    data object ToggleCraft : StorageAction

    data object OpenSwitcher : StorageAction

    data object CraftToStorage : StorageAction

    data object CraftToInventory : StorageAction

    data object ToggleCraftReturn : StorageAction
}

/**
 * 存储终端的游戏线程逻辑，行为与旧版界面一致：搜索与配方查看器同步、排序、行数、合成与网络切换。
 * [extraHeight] 是存储网格与背包之外的附加区域（合成区）的高度，用于按窗口高度推算可显示的行数。
 */
class StorageController<M : DimensionsNetMenu>(
    private val menu: M,
    private val extraHeight: Int,
    private val craftToggle: Boolean = true,
) :
    BdController<StorageState, StorageAction>(StorageState(menu.lines, 0, 0, "", 0, emptyList())) {
    private var search = CommonConfigRuntime.uiSearch
    private var searchRevision = 0
    private var sort = CommonConfigRuntime.uiSortButton
    private var secondSort = CommonConfigRuntime.uiSecondSortButton
    private var reverse = CommonConfigRuntime.uiReverseButton
    private var searchMode = CommonConfigRuntime.uiSearchButton
    private var craftReturn = CommonConfigRuntime.uiCraftReturnButton

    init {
        if (UIDataHelper.isTransfer) {
            menu.lineData = UIDataHelper.currentPage
            UIDataHelper.lastMousePos?.let { GLFW.glfwSetCursorPos(window(), it.x.toDouble(), it.y.toDouble()) }
            UIDataHelper.isTransfer = false
        }
        val maxLines = maxLines()
        if (maxLines < menu.lines) {
            // 自动计算的行数不写入配置
            menu.lines = maxOf(maxLines, 2)
            menu.rebuildSlots()
        }
        applySearch(search)
        // 合成界面打开时同步合成产物的放回优先级
        if (menu is DimensionsCraftMenu) menu.writeAndSendQuickData()
        ui.update(snapshot())
    }

    private fun window() = Minecraft.getInstance().window.window

    /** 与旧版相同：窗口高度减去上下各 18 的留白后，面板能容纳的行数 */
    private fun maxLines(): Int =
        ((Minecraft.getInstance().window.guiScaledHeight - 36 - storagePanelHeight(0, extraHeight)) / ROW.toFloat()).toInt()

    private fun imageHeight() = storagePanelHeight(menu.lines, extraHeight)

    override fun snapshot(): StorageState {
        pullRecipeViewerSearch()
        return StorageState(menu.lines, menu.lineData, menu.maxLineData, search, searchRevision, buttons(), craftButtons())
    }

    private fun buttons(): List<Pair<StorageAction, SideButton>> = buildList {
        add(StorageAction.CycleSort to SideButton(BdIcons.sprite(sortIcon(sort)), tr("tooltip.button.beyonddimensions.${sortKey(sort)}")))
        add(
            StorageAction.CycleSecondSort to
                SideButton(BdIcons.sprite(sortIcon(secondSort)), tr("tooltip.button.beyonddimensions.${sortKey(secondSort)}_second"))
        )
        val descending = reverse == ButtonState.ENABLED
        add(
            StorageAction.ToggleReverse to
                SideButton(
                    BdIcons.sprite(if (descending) "sort_desc" else "sort_asc"),
                    tr(if (descending) "tooltip.button.beyonddimensions.sort_desc" else "tooltip.button.beyonddimensions.sort_asc"),
                )
        )
        val searchEnabled = searchMode == ButtonState.ENABLED
        add(
            StorageAction.ToggleSearchMode to
                SideButton(
                    BdIcons.sprite(if (searchEnabled) "search_enable" else "search_disable"),
                    tr(if (searchEnabled) "tooltip.button.beyonddimensions.search_enable" else "tooltip.button.beyonddimensions.search_disable"),
                )
        )
        add(StorageAction.AddRow to SideButton(BdIcons.sprite("up_arrow"), tr("tooltip.button.beyonddimensions.add_page")))
        add(StorageAction.RemoveRow to SideButton(BdIcons.sprite("down_arrow"), tr("tooltip.button.beyonddimensions.remove_page")))
        if (craftToggle)
            add(StorageAction.ToggleCraft to SideButton(BdIcons.sprite("craft_button"), tr("tooltip.button.beyonddimensions.craft_toggle")))
        if (!ClientConfigRuntime.disableMultiNetworkSwitching)
            add(
                StorageAction.OpenSwitcher to
                    SideButton(BdIcons.sprite("opposite_arrow"), tr("tooltip.button.beyonddimensions.open_primary_net_switcher"))
            )
    }

    private fun craftButtons(): List<Pair<StorageAction, CraftButton>> {
        if (menu !is DimensionsCraftMenu) return emptyList()
        val storageFirst = craftReturn == ButtonState.ENABLED
        return listOf(
            StorageAction.CraftToStorage to CraftButton(BdIcons.sprite("up_arrow"), tr("tooltip.button.beyonddimensions.transfer_to_storage")),
            StorageAction.CraftToInventory to CraftButton(BdIcons.sprite("down_arrow"), tr("tooltip.button.beyonddimensions.transfer_to_inv")),
            StorageAction.ToggleCraftReturn to
                CraftButton(
                    BdIcons.sprite(if (storageFirst) "sort_asc" else "sort_desc"),
                    tr(if (storageFirst) "tooltip.button.beyonddimensions.first_storage" else "tooltip.button.beyonddimensions.first_inv"),
                ),
        )
    }

    override fun handle(action: StorageAction) {
        val config = Config.INSTANCE.commonConfig
        when (action) {
            is StorageAction.Search -> applySearch(action.text)
            is StorageAction.ScrollTo -> {
                val line = action.line.coerceIn(0, menu.maxLineData)
                if (menu.lineData != line) {
                    menu.lineData = line
                    menu.buildIndexList()
                }
            }
            StorageAction.CycleSort -> {
                sort = next(SORTS, sort)
                CommonConfigRuntime.uiSortButton = sort
                config.UI_SORT_BUTTON.set(sort)
                config.UI_SORT_BUTTON.save()
                menu.buildIndexList()
            }
            StorageAction.CycleSecondSort -> {
                secondSort = next(SORTS, secondSort)
                CommonConfigRuntime.uiSecondSortButton = secondSort
                config.UI_SECOND_SORT_BUTTON.set(secondSort)
                config.UI_SECOND_SORT_BUTTON.save()
                menu.buildIndexList()
            }
            StorageAction.ToggleReverse -> {
                reverse = next(TOGGLE, reverse)
                CommonConfigRuntime.uiReverseButton = reverse
                config.UI_REVERSE_BUTTON.set(reverse)
                config.UI_REVERSE_BUTTON.save()
                menu.buildIndexList()
            }
            StorageAction.ToggleSearchMode -> {
                searchMode = next(TOGGLE, searchMode)
                CommonConfigRuntime.uiSearchButton = searchMode
                config.UI_SEARCH_BUTTON.set(searchMode)
                config.UI_SEARCH_BUTTON.save()
            }
            StorageAction.AddRow -> {
                if (Minecraft.getInstance().window.guiScaledHeight - 36 <= imageHeight() + 18 || menu.lines >= 99) return
                menu.addLines()
                saveLines()
            }
            StorageAction.RemoveRow -> {
                if (menu.lines <= 2) return
                menu.reduceLines()
                saveLines()
            }
            StorageAction.ToggleCraft -> {
                saveTransferContext()
                val craft = menu is DimensionsCraftMenu
                val state = if (craft) ButtonState.DISABLED else ButtonState.ENABLED
                CommonConfigRuntime.uiCraftButton = state
                config.UI_CRAFT_BUTTON.set(state)
                config.UI_CRAFT_BUTTON.save()
                PacketDistributor.sendToServer(
                    OpenNetGuiPacket(menu.player.stringUUID, if (craft) NetMenuType.NET_MENU else NetMenuType.NET_CRAFT_MENU)
                )
            }
            StorageAction.OpenSwitcher -> {
                saveTransferContext()
                PacketDistributor.sendToServer(OpenPrimaryNetSwitcherPacket())
            }
            StorageAction.CraftToStorage -> PacketDistributor.sendToServer(ClickTransferCraftButtonPacket(true))
            StorageAction.CraftToInventory -> PacketDistributor.sendToServer(ClickTransferCraftButtonPacket(false))
            StorageAction.ToggleCraftReturn -> {
                craftReturn = next(listOf(ButtonState.ENABLED, ButtonState.DISABLED), craftReturn)
                CommonConfigRuntime.uiCraftReturnButton = craftReturn
                config.UI_CRAFT_RETURN_BUTTON.set(craftReturn)
                config.UI_CRAFT_RETURN_BUTTON.save()
                menu.writeAndSendQuickData()
            }
        }
    }

    private fun saveLines() {
        CommonConfigRuntime.uiPageNum = menu.lines
        Config.INSTANCE.commonConfig.UI_PAGE_NUM.set(menu.lines)
        Config.INSTANCE.commonConfig.UI_PAGE_NUM.save()
        CommonConfigRuntime.uiSearch = search
        menu.rebuildSlots()
        menu.buildIndexList()
    }

    private fun applySearch(text: String) {
        search = text
        menu.loadSearchText(text)
        CommonConfigRuntime.uiSearch = text
        menu.markForceAllUpdateClientView()
        menu.updateViewerStorage(false)
        if (!CommonConfigRuntime.searchTextWithJEIEMI) return
        if (ModPresence.isLoaded(OtherModIds.JEI))
            BDjeiPlugin.runtime().ifPresent { runtime ->
                val filter = runtime.ingredientFilter
                if (filter.filterText != text) filter.filterText = text
            }
        if (ModPresence.isLoaded(OtherModIds.EMI) && EmiApi.getSearchText() != text) EmiApi.setSearchText(text)
    }

    /** 每刻从配方查看器同步一次搜索文字 */
    private fun pullRecipeViewerSearch() {
        if (!CommonConfigRuntime.searchTextWithJEIEMI) return
        var external: String? = null
        if (ModPresence.isLoaded(OtherModIds.JEI))
            BDjeiPlugin.runtime().ifPresent { runtime -> runtime.ingredientFilter.filterText.takeIf { it != search }?.let { external = it } }
        if (ModPresence.isLoaded(OtherModIds.EMI)) EmiApi.getSearchText().takeIf { it != search }?.let { external = it }
        external?.let {
            applySearch(it)
            searchRevision++
        }
    }

    private fun saveTransferContext() {
        UIDataHelper.currentPage = menu.lineData
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        GLFW.glfwGetCursorPos(window(), x, y)
        UIDataHelper.lastMousePos = Vec2(x[0].toFloat(), y[0].toFloat())
        UIDataHelper.isTransfer = true
    }

    private companion object {
        val SORTS =
            listOf(
                ButtonState.SORT_CREATIVE_TAB,
                ButtonState.SORT_MAX_STACK,
                ButtonState.SORT_QUANTITY,
                ButtonState.SORT_NAME,
                ButtonState.SORT_MODID,
                ButtonState.SORT_INSERTED_TIME,
                ButtonState.SORT_MODIFIED_TIME,
            )
        val TOGGLE = listOf(ButtonState.DISABLED, ButtonState.ENABLED)

        fun next(states: List<ButtonState>, current: ButtonState) = states[(states.indexOf(current) + 1).mod(states.size)]

        fun sortKey(state: ButtonState) = state.name.lowercase()

        fun sortIcon(state: ButtonState) = state.name.lowercase()
    }
}
