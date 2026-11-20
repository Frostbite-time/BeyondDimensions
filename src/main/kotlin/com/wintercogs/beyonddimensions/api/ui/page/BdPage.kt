package com.wintercogs.beyonddimensions.api.ui.page

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.wintercogs.beyonddimensions.client.ui.kit.BdPixelIcon
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.forge.sync.MenuAction
import dev.compixel.forge.sync.MenuSync
import dev.compixel.forge.sync.SyncedMenu
import dev.compixel.host.GameThread
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu

/**
 * 附属模组注入 BD 界面的一页，用 [BdPages.register] 注册。
 *
 * 页面与宿主界面一起运行在游戏线程。会变化的游戏数据在 [Content] 中通过 `gameState { ... }` 读取
 * （返回不可变值，不要放菜单、物品堆等游戏对象）；修改游戏状态或向服务端发请求的操作通过
 * [BdPageScope.gameThread] 提交。
 *
 * @param M 页面所在界面的菜单类型，注册给父类的页面也出现在子类菜单的界面上
 */
interface BdPage<M : AbstractContainerMenu> {
    /** 竖条上的图标，一格一个界面像素，10×10 与 BD 自己的页签一致，例如 `BdIcons.Settings` 或自己画的 [BdPixelIcon] */
    val icon: BdPixelIcon

    /** 页签的悬停提示；打开界面时取一次 */
    fun title(context: BdPageContext<M>): Component

    /** 这个界面是否显示这一页；打开界面时判断一次 */
    fun appliesTo(context: BdPageContext<M>): Boolean = true

    /** 这一页的大小，默认与主页面一样大；打开界面时取一次 */
    fun size(context: BdPageContext<M>): BdPageSize = BdPageSize.Main

    /** 页面内容；可以直接使用 BD 的界面组件，通过 `gameState` 读取游戏数据、通过 [BdPageScope.gameThread] 提交操作 */
    @Composable
    fun ColumnScope.Content(context: BdPageContext<M>, scope: BdPageScope)
}

/** 页面访问宿主界面与菜单的上下文 */
class BdPageContext<out M : AbstractContainerMenu> internal constructor(
    /** 界面的菜单（客户端一侧） */
    val menu: M,
    /** 界面本身 */
    val screen: Screen,
    private val close: () -> Unit,
) {
    val minecraft: Minecraft
        get() = Minecraft.getInstance()

    val player: LocalPlayer
        get() = checkNotNull(minecraft.player)

    /** 关闭界面 */
    fun requestClose() = close()

    /** 把注册在 [com.wintercogs.beyonddimensions.api.menu.BdMenuActions] 的动作送到服务器；返回 false 表示没有发出 */
    fun <V> request(action: MenuAction<in M, V>, value: V): Boolean {
        val synced = menu as? SyncedMenu ?: return false
        @Suppress("UNCHECKED_CAST") val sync = synced.menuSync() as MenuSync<M>
        return sync.hasSnapshot() && sync.request(action, value).queued()
    }
}

/** 注入页在 [BdPage.Content] 中可以使用的宿主能力 */
interface BdPageScope {
    /** 这一页是否正在显示；为 false 时控件应停止响应 */
    val shown: Boolean

    /**
     * 界面的槽位，没有槽位的界面为 null。用 `BdSlot(slots, id)` 画出菜单的槽位；
     * id 是菜单 `slots` 列表的下标
     */
    val slots: ComposeMenuSlots<*>?

    /** 宿主界面的游戏线程任务入口，用于在 UI 回调中提交修改游戏状态或服务端请求的操作 */
    val gameThread: GameThread

    /** 切到另一页：[BdPages.MAIN]、[BdPages.SETTINGS] 或某个注入页的 id */
    fun open(page: String)

    /** 关闭界面 */
    fun requestClose()
}
