package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import com.wintercogs.beyonddimensions.api.ui.page.*
import com.wintercogs.beyonddimensions.client.ui.kit.BdRailTab
import com.wintercogs.beyonddimensions.client.ui.kit.BdTabPage
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.GameThread
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

@Composable
fun rememberPageSelection() = remember { BdPageSelection() }

/**
 * 一个界面上由附属模组注入的页：用 [InjectedTabs] 画页签、[InjectedPages] 画页面。
 * 注入页直接共享宿主界面的 `gameState` 与 [gameThread]，无需单独维护状态绑定
 */
class BdPageHost<M : AbstractContainerMenu>(
    menu: M,
    screen: Screen,
    private val slots: ComposeMenuSlots<*>?,
    private val gameThread: GameThread,
    private val requestClose: () -> Unit,
) {
    private val context = BdPageContext(menu, screen, requestClose)
    private val mounted: List<Mounted> = BdPages.pagesFor(menu).mapNotNull { mount(it) }

    val isEmpty
        get() = mounted.isEmpty()

    @Suppress("UNCHECKED_CAST")
    private fun mount(entry: BdPages.Entry): Mounted? {
        val page = entry.page as BdPage<M>
        if (!page.appliesTo(context)) return null
        return Mounted(
            entry.id.toString(),
            page,
            page.title(context).string,
            page.size(context),
        )
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

    private inner class Mounted(
        val id: String,
        val page: BdPage<M>,
        val title: String,
        val size: BdPageSize,
    ) {
        @Composable
        fun Show(box: BoxScope, selection: BdPageSelection) {
            val shown = selection.current == id
            with(box) {
                BdTabPage(shown, size = size) {
                    val scope = remember(shown, selection) { Scope(shown, selection) }
                    with(page) { Content(context, scope) }
                }
            }
        }

        private inner class Scope(override val shown: Boolean, private val selection: BdPageSelection) : BdPageScope {
            override val slots
                get() = this@BdPageHost.slots

            override val gameThread
                get() = this@BdPageHost.gameThread

            override fun open(page: String) = selection.open(page)

            override fun requestClose() = this@BdPageHost.requestClose()
        }
    }
}

/** 注入页的页签，放在界面自带的页签之后 */
@Composable
fun InjectedTabs(host: BdPageHost<*>, selection: BdPageSelection) = host.Tabs(selection)

/** 注入页的页面 */
@Composable
fun BoxScope.InjectedPages(host: BdPageHost<*>, selection: BdPageSelection) = with(host) { Pages(selection) }
