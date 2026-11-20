package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.mojang.logging.LogUtils
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanel
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanelContext
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanelScope
import com.wintercogs.beyonddimensions.api.ui.storage.StorageSidePanels
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.GameThread
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

/** 当前展开的侧边面板快照：供 Compose 在状态变化与收起动画期间读取 */
@Immutable
internal data class ActiveSidePanel(
    val id: ResourceLocation,
    val contentWidth: Int,
    val totalWidth: Int,
    val openSerial: Long,
)

/**
 * 存储界面上挂载的右侧可展开侧边面板宿主。
 * 同一时间最多展开一个面板，展开/收起或切换时回调 [onPanelChanged] 触发网格列数重排。
 */
internal class StorageSidePanelHost(
    menu: DimensionsNetMenu,
    screen: Screen,
    private val slots: ComposeMenuSlots<*>,
    private val gameThread: GameThread,
    private val requestClose: () -> Unit,
    private val onPanelChanged: () -> Unit,
) {
    private val mounted = LinkedHashMap<ResourceLocation, Mounted>()

    private var active: Mounted? = null
    private var openSerial: Long = 0L

    init {
        for ((id, panel) in StorageSidePanels.all()) {
            val context = StorageSidePanelContext(id, menu, screen, requestClose)
            if (!panel.appliesTo(context)) continue
            val requested = panel.width(context)
            val width = requested.coerceIn(StorageSidePanels.MIN_WIDTH, StorageSidePanels.MAX_WIDTH)
            if (requested != width) {
                logger.warn(
                    "Storage side panel {} requested width {} dp, clamped to {}..{} ({} dp)",
                    id, requested, StorageSidePanels.MIN_WIDTH, StorageSidePanels.MAX_WIDTH, width,
                )
            }
            mounted[id] = Mounted(id, panel, context, width)
        }
    }

    val activeId: ResourceLocation?
        get() = active?.id

    val activeTotalWidth: Int
        get() = active?.let { SIDE_PANEL_CHROME_WIDTH + it.width } ?: 0

    fun activeState(): ActiveSidePanel? =
        active?.let { ActiveSidePanel(it.id, it.width, SIDE_PANEL_CHROME_WIDTH + it.width, openSerial) }

    fun open(id: ResourceLocation): Boolean {
        val target = mounted[id] ?: return false
        val previous = active
        if (previous === target) {
            // 已经展开时再次请求，刷新 serial 以确保若当前在设置页也会切回主页面
            openSerial++
            return true
        }
        active = target
        openSerial++
        previous?.panel?.onClose(previous.context)
        target.panel.onOpen(target.context)
        onPanelChanged()
        return true
    }

    fun close() {
        val previous = active ?: return
        active = null
        previous.panel.onClose(previous.context)
        onPanelChanged()
    }

    /** 存储界面关闭时通知当前展开的面板收尾，不再重算视口 */
    fun closeOnScreenClosed() {
        val previous = active ?: return
        active = null
        previous.panel.onClose(previous.context)
    }

    @Composable
    fun ColumnScope.Content(id: ResourceLocation, shown: Boolean) {
        val entry = mounted[id] ?: return
        val scope = remember(shown) {
            object : StorageSidePanelScope {
                override val shown: Boolean = shown
                override val slots: ComposeMenuSlots<*>
                    get() = this@StorageSidePanelHost.slots
                override val gameThread: GameThread
                    get() = this@StorageSidePanelHost.gameThread

                override fun openPanel(id: ResourceLocation) {
                    gameThread.post { open(id) }
                }

                override fun closePanel() {
                    gameThread.post { close() }
                }

                override fun requestClose() = this@StorageSidePanelHost.requestClose()
            }
        }
        key(id) {
            with(entry.panel) { Content(entry.context, scope) }
        }
    }

    private class Mounted(
        val id: ResourceLocation,
        val panel: StorageSidePanel,
        val context: StorageSidePanelContext,
        val width: Int,
    )

    private companion object {
        val logger = LogUtils.getLogger()
    }
}
