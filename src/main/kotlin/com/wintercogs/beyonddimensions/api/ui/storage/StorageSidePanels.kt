package com.wintercogs.beyonddimensions.api.ui.storage

import com.wintercogs.beyonddimensions.api.ids.BDConstants
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

/**
 * 存储界面右侧可展开区域的侧边面板注册表与开关入口。
 *
 * 在客户端的模组构造函数或初始化事件里注册：
 * ```
 * StorageSidePanels.register(ResourceLocation.fromNamespaceAndPath("example", "bookmarks"), BookmarksSidePanel)
 * ```
 * 展开与收起的触发方式完全由附属自行决定（例如在工作台、注入页或自定义按钮的点击回调里调用 [open] / [toggle] / [close]）。
 */
object StorageSidePanels {
    /** 侧边面板内容区的最小宽度（dp） */
    const val MIN_WIDTH = 80

    /** 侧边面板内容区的默认宽度（dp），与内置分类编辑区（7 列标记格）一致 */
    const val DEFAULT_WIDTH = 127

    /** 侧边面板内容区的最大宽度（dp） */
    const val MAX_WIDTH = 220

    /** BD 内置的分类编辑侧边面板 */
    @JvmField
    val CATEGORY_EDITOR: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "category_editor")

    @Volatile
    private var entries = emptyMap<ResourceLocation, StorageSidePanel>()

    /** 注册一个存储界面的右侧展开面板 [id] */
    @JvmStatic
    @Synchronized
    fun register(id: ResourceLocation, panel: StorageSidePanel) {
        check(id !in entries) { "Storage side panel $id is already registered" }
        entries = entries + (id to panel)
    }

    /** 已注册的侧边面板 [id]，未注册时为 null */
    @JvmStatic
    fun get(id: ResourceLocation): StorageSidePanel? = entries[id]

    internal fun all(): List<Pair<ResourceLocation, StorageSidePanel>> = entries.entries.map { it.key to it.value }

    /** 在存储界面 [screen] 上展开侧边面板 [id]（需在客户端线程调用）；返回 false 表示 [screen] 不是存储界面或该面板未挂载 */
    @JvmStatic
    fun open(screen: Screen, id: ResourceLocation): Boolean =
        (screen as? StorageSidePanelOwner)?.openSidePanel(id) ?: false

    /** 收起存储界面 [screen] 当前展开的侧边面板（需在客户端线程调用） */
    @JvmStatic
    fun close(screen: Screen) {
        (screen as? StorageSidePanelOwner)?.closeSidePanel()
    }

    /** 切换侧边面板 [id] 的展开状态（需在客户端线程调用）；返回切换后该面板是否处于展开状态 */
    @JvmStatic
    fun toggle(screen: Screen, id: ResourceLocation): Boolean {
        val owner = screen as? StorageSidePanelOwner ?: return false
        if (owner.currentSidePanel() == id) {
            owner.closeSidePanel()
            return false
        }
        return owner.openSidePanel(id)
    }

    /** 存储界面 [screen] 当前展开的侧边面板 ID，未展开或不是存储界面时为 null */
    @JvmStatic
    fun current(screen: Screen): ResourceLocation? = (screen as? StorageSidePanelOwner)?.currentSidePanel()
}
