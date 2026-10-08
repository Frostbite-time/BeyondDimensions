package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wintercogs.beyonddimensions.api.ui.page.BdPage
import com.wintercogs.beyonddimensions.api.ui.page.BdPageContext
import com.wintercogs.beyonddimensions.api.ui.page.BdPageScope
import com.wintercogs.beyonddimensions.api.ui.page.BdPages
import com.wintercogs.beyonddimensions.client.ui.kit.BdRailTab
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabPage
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.inventory.AbstractContainerMenu

/** 界面当前打开的页：[BdPages.MAIN]、[BdPages.SETTINGS] 或注入页的 id */
@Stable
class BdPageSelection {
    var current by mutableStateOf(BdPages.MAIN)
        private set

    val main
        get() = current == BdPages.MAIN

    val settings
        get() = current == BdPages.SETTINGS

    fun open(page: String) {
        current = page
    }
}

@Composable fun rememberPageSelection() = remember { BdPageSelection() }

/**
 * 一个界面上由附属模组注入的页。界面构造时按菜单找出注册的页，在游戏线程取快照、处理动作，
 * 在界面的竖条上用 [InjectedTabs] 画页签，在页面区用 [InjectedPages] 画页面。
 *
 * 每页一条 [UiBinding]：在游戏线程构造、更新与关闭，Compose 线程只读状态、送动作
 */
class BdPageHost<M : AbstractContainerMenu>(
    menu: M,
    screen: Screen,
    private val slots: ComposeMenuSlots<*>?,
    private val requestClose: () -> Unit,
) : AutoCloseable {
    private val context = BdPageContext(menu, screen, requestClose)
    private val mounted: List<Mounted<*, *>> = BdPages.pagesFor(menu).mapNotNull { mount(it) }
    private var closed = false

    val isEmpty
        get() = mounted.isEmpty()

    @Suppress("UNCHECKED_CAST")
    private fun mount(entry: BdPages.Entry): Mounted<*, *>? {
        val page = entry.page as BdPage<M, Any?, Any?>
        if (!page.appliesTo(context)) return null
        return Mounted(entry.id.toString(), page, page.title(context).string, UiBinding(page.snapshot(context)))
    }

    /** 游戏线程：每刻与每个输入事件之后调用，先处理各页送来的动作，再取新快照 */
    fun update() {
        if (closed) return
        for (page in mounted) page.update()
    }

    override fun close() {
        if (closed) return
        closed = true
        for (page in mounted) page.binding.close()
    }

    @Composable
    internal fun Tabs(selection: BdPageSelection) {
        for (page in mounted) {
            BdRailTab(page.title, page.page.icon, selected = selection.current == page.id) { selection.open(page.id) }
        }
    }

    @Composable
    internal fun BoxScope.Pages(selection: BdPageSelection) {
        for (page in mounted) page.Show(this, selection)
    }

    private inner class Mounted<S, A>(
        val id: String,
        val page: BdPage<M, S, A>,
        val title: String,
        val binding: UiBinding<S, A>,
    ) {
        fun update() {
            binding.drainActions { page.handle(context, it) }
            binding.update(page.snapshot(context))
        }

        @Composable
        fun Show(box: BoxScope, selection: BdPageSelection) {
            val shown = selection.current == id
            with(box) {
                BdTabPage(shown) {
                    val scope = remember(shown) { Scope(shown, selection) }
                    with(page) { Content(binding.value, scope) }
                }
            }
        }

        private inner class Scope(override val shown: Boolean, private val selection: BdPageSelection) :
            BdPageScope<A> {
            override val slots
                get() = this@BdPageHost.slots

            override fun send(action: A) = binding.send(action)

            override fun open(page: String) = selection.open(page)

            override fun requestClose() = this@BdPageHost.requestClose()
        }
    }
}

/** 注入页的页签，放在界面自带的页签之后 */
@Composable fun InjectedTabs(host: BdPageHost<*>, selection: BdPageSelection) = host.Tabs(selection)

/** 注入页的页面，放在 [com.wintercogs.beyonddimensions.client.ui.kit.BdTabbedWindow] 的页面区里 */
@Composable fun BoxScope.InjectedPages(host: BdPageHost<*>, selection: BdPageSelection) = with(host) { Pages(selection) }
