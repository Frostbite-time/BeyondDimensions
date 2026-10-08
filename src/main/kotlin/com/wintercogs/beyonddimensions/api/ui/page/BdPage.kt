package com.wintercogs.beyonddimensions.api.ui.page

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.forge.sync.MenuAction
import dev.compixel.forge.sync.MenuSync
import dev.compixel.forge.sync.SyncedMenu
import dev.compixel.ui.ore.display.OrePixelArt
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu

/**
 * 附属模组注入 BD 界面的一页：窗口左侧竖条上的一个页签，点开后是这一页。用 [BdPages.register] 注册。
 *
 * 与 BD 自己的界面一样分两半：[snapshot] 与 [handle] 在游戏线程运行，可以读写菜单、玩家与世界；
 * [Content] 在 Compose 线程运行，只能用 [snapshot] 给出的状态，通过 [BdPageScope.send] 把动作交回游戏线程。
 * 快照应当是不可变的值，相等的快照不会让页面重绘；不要把菜单、物品堆等游戏对象放进快照。
 *
 * 界面打开时每页各取一次快照，之后每刻一次，处理完输入事件里送来的动作后也会再取。
 * 需要改动服务器上的东西时，在 [handle] 里用 [BdPageContext.request] 发送注册在
 * [com.wintercogs.beyonddimensions.api.menu.BdMenuActions] 的动作，或发送自己的网络包。
 *
 * @param M 页面所在界面的菜单类型，注册给父类的页面也出现在子类菜单的界面上
 * @param S 页面状态
 * @param A 页面动作
 */
interface BdPage<M : AbstractContainerMenu, S, A> {
    /** 竖条上的图标，9×9 显示，例如 `OreGlyph.Gear.art` 或自己画的 [OrePixelArt] */
    val icon: OrePixelArt

    /** 页签的悬停提示；打开界面时在游戏线程取一次 */
    fun title(context: BdPageContext<M>): Component

    /** 这个界面是否显示这一页，例如只给某种机器或某个模式显示；打开界面时在游戏线程判断一次 */
    fun appliesTo(context: BdPageContext<M>): Boolean = true

    /** 在游戏线程读取页面状态；可能一刻多次调用，只读不改 */
    fun snapshot(context: BdPageContext<M>): S

    /** 在游戏线程处理页面送来的动作，处理完会再取快照 */
    fun handle(context: BdPageContext<M>, action: A) {}

    /**
     * 页面内容，在 Compose 线程组合。与 BD 的设置页相同：有页边距，内容超出高度时在页内滚动，
     * 可以直接使用 BD 的界面组件（`BdSectionLabel`、`BdSettingRow`、`BdToggle` 等）。
     */
    @Composable
    fun ColumnScope.Content(state: S, scope: BdPageScope<A>)
}

/**
 * 页面在游戏线程上的上下文：所在界面的菜单与界面本身。只在 [BdPage.title]、[BdPage.appliesTo]、[BdPage.snapshot]、
 * [BdPage.handle] 中使用，不要带进 Compose。
 */
class BdPageContext<out M : AbstractContainerMenu> internal constructor(
    /** 界面的菜单；客户端的菜单，数据由服务器同步过来 */
    val menu: M,
    /** 界面本身，例如打开子界面时作为返回的上一级 */
    val screen: Screen,
    private val close: () -> Unit,
) {
    val minecraft: Minecraft
        get() = Minecraft.getInstance()

    val player: LocalPlayer
        get() = checkNotNull(minecraft.player)

    /** 关闭界面 */
    fun requestClose() = close()

    /**
     * 把一个菜单动作送到服务器，由动作的处理器带着服务器上的菜单与玩家执行。动作要先在两端注册到
     * [com.wintercogs.beyonddimensions.api.menu.BdMenuActions]。返回 false 表示动作没有排进发送队列，
     * 例如菜单还没有收到服务器的第一份数据，或动作积压过多
     */
    fun <V> request(action: MenuAction<in M, V>, value: V): Boolean {
        val synced = menu as? SyncedMenu ?: return false
        @Suppress("UNCHECKED_CAST") val sync = synced.menuSync() as MenuSync<M>
        return sync.hasSnapshot() && sync.request(action, value).queued()
    }
}

/** 页面在 Compose 线程上能做的事 */
interface BdPageScope<A> {
    /** 这一页是否正在显示；切到别的页后，淡出期间为 false，控件此时应停止响应 */
    val shown: Boolean

    /**
     * 界面的槽位；没有槽位的界面（网络控制器、主网络切换）为 null。用 `BdSlot(slots, id)` 在这一页上画出菜单的槽位，
     * 可以照常点击、拖动、Shift 移动：槽位只在当前页上响应，主页面上的同一个槽位（例如玩家背包）此时不会抢走点击。
     * 槽位编号是菜单 `slots` 列表的下标，在游戏线程（例如 [BdPage.snapshot] 或构造页面时）取出后放进状态
     */
    val slots: ComposeMenuSlots<*>?

    /** 把动作交给 [BdPage.handle]。界面已关闭或动作积压过多时返回 false，动作不会被覆盖 */
    fun send(action: A): Boolean

    /** 切到另一页：[BdPages.MAIN]、[BdPages.SETTINGS] 或某个注入页的 id */
    fun open(page: String)

    /** 关闭界面 */
    fun requestClose()
}
