package com.wintercogs.beyonddimensions.api.ui.storage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.forge.sync.MenuAction
import dev.compixel.host.GameThread
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.ResourceLocation

/**
 * 存储界面右侧可展开区域里的一个侧边面板，用 [StorageSidePanels.register] 注册。
 * 展开与收起的入口由附属自行决定，在需要时通过 [StorageSidePanels.open]、[StorageSidePanels.close] 或
 * [StorageSidePanels.toggle] 控制；同一时间最多展开一个面板，面板展开时窗口向右扩展并自动收窄存储格列数。
 *
 * 侧边面板与存储界面一起运行在游戏线程。会变化的游戏数据在 [Content] 中通过 `gameState { ... }` 读取
 * （返回不可变值，不要放菜单、物品堆等游戏对象）；修改游戏状态或向服务端发请求的操作通过
 * [StorageSidePanelScope.gameThread] 提交。内容放在宿主提供的竖向滚动容器内，可以直接使用 BD 的界面组件。
 */
interface StorageSidePanel {
    /** 这个存储界面是否挂载此面板；打开界面时判断一次 */
    fun appliesTo(context: StorageSidePanelContext): Boolean = true

    /**
     * 面板内容区的宽度，单位 dp，不含左侧的分隔线与间距；打开界面时取一次。
     * 超出 [StorageSidePanels.MIN_WIDTH]..[StorageSidePanels.MAX_WIDTH] 的值会被限制在该范围内
     */
    fun width(context: StorageSidePanelContext): Int = StorageSidePanels.DEFAULT_WIDTH

    /** 面板展开时调用（运行在游戏线程） */
    fun onOpen(context: StorageSidePanelContext) {}

    /** 面板收起、被切换到其他面板或存储界面关闭时调用（运行在游戏线程） */
    fun onClose(context: StorageSidePanelContext) {}

    /** 面板的内容，位于可滚动的 [ColumnScope] 中；通过 `gameState` 读取游戏数据，通过 [StorageSidePanelScope.gameThread] 提交操作 */
    @Composable
    fun ColumnScope.Content(context: StorageSidePanelContext, scope: StorageSidePanelScope)
}

/** 侧边面板访问存储界面与菜单的上下文，每个面板在同一界面中各有一份 */
class StorageSidePanelContext internal constructor(
    /** 面板的 ID */
    val id: ResourceLocation,
    /** 存储界面的菜单（客户端一侧） */
    val menu: DimensionsNetMenu,
    /** 界面本身 */
    val screen: Screen,
    private val close: () -> Unit,
) {
    private val values = HashMap<Any, Any?>()

    val minecraft: Minecraft
        get() = Minecraft.getInstance()

    val player: LocalPlayer
        get() = checkNotNull(minecraft.player)

    /** 关闭整个存储界面 */
    fun requestClose() = close()

    /** 本面板在这个界面里按 [key] 保存的值，第一次取时由 [create] 生成；界面关闭后丢弃 */
    @Suppress("UNCHECKED_CAST")
    fun <T> remember(key: Any, create: () -> T): T = values.getOrPut(key, create) as T

    /** 把注册在 [com.wintercogs.beyonddimensions.api.menu.BdMenuActions] 或菜单部件里的动作送到服务器；返回 false 表示没有发出 */
    fun <V> request(action: MenuAction<in DimensionsNetMenu, V>, value: V): Boolean {
        val sync = menu.menuSync()
        return sync.hasSnapshot() && sync.request(action, value).queued()
    }
}

/** 侧边面板在 [StorageSidePanel.Content] 中可以使用的宿主能力 */
interface StorageSidePanelScope {
    /** 这个面板是否正在主页面上处于展开状态；为 false（例如收起动画期间或切到设置页时）控件应停止响应 */
    val shown: Boolean

    /** 界面的槽位。用 `BdSlot(slots, id)` 画出菜单的槽位 */
    val slots: ComposeMenuSlots<*>

    /** 宿主界面的游戏线程任务入口，用于在 UI 回调中提交修改游戏状态或服务端请求的操作 */
    val gameThread: GameThread

    /** 切换到另一个已注册的侧边面板（通过 [gameThread] 提交）；未挂载在此界面时无效果 */
    fun openPanel(id: ResourceLocation)

    /** 收起当前侧边面板（通过 [gameThread] 提交） */
    fun closePanel()

    /** 关闭整个存储界面 */
    fun requestClose()
}

/** 由存储界面实现的内部宿主接口，供 [StorageSidePanels] 控制当前打开的侧边面板 */
internal interface StorageSidePanelOwner {
    fun openSidePanel(id: ResourceLocation): Boolean

    fun closeSidePanel()

    fun currentSidePanel(): ResourceLocation?
}
