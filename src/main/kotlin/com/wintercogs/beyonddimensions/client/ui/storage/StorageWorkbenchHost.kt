package com.wintercogs.beyonddimensions.client.ui.storage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import com.mojang.logging.LogUtils
import com.wintercogs.beyonddimensions.Config
import com.wintercogs.beyonddimensions.api.menu.widget.ClearableWidget
import com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbench
import com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbenchContext
import com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbenchScope
import com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbenches
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation

/** 下拉框里的一个工作台，打开界面时在游戏线程取一次；[holdsItems] 时它的菜单部件放着玩家的物品，显示退回方向按钮 */
@Immutable
data class WorkbenchInfo(val id: String, val title: String, val holdsItems: Boolean)

/**
 * 一个存储界面上可用的工作台，按注册顺序：服务端打开菜单时判断出哪些可用，界面只挂上它们。
 * 在游戏线程取快照、处理动作，用 [Content] 画出其中一个
 */
class StorageWorkbenchHost(
    menu: DimensionsNetMenu,
    screen: Screen,
    private val slots: ComposeMenuSlots<*>,
    private val requestClose: () -> Unit,
) : AutoCloseable {
    private val mounted = LinkedHashMap<String, Mounted<*, *>>()
    private var closed = false

    /** 这个界面上有的工作台，按下拉框的顺序 */
    val info: List<WorkbenchInfo>

    /** 工作台内容的宽高：所有工作台里最宽与最高的，切换工作台时工作区大小不变 */
    val contentWidth: Int
    val contentHeight: Int

    init {
        val described = ArrayList<WorkbenchInfo>()
        var width = 0
        var height = 0
        for (workbenchId in menu.workbenches()) {
            @Suppress("UNCHECKED_CAST") val workbench =
                StorageWorkbenches.get(workbenchId) as StorageWorkbench<Any?, Any?>?
            if (workbench == null) {
                logger.warn("Storage workbench {} has no UI registered on the client; it is left out", workbenchId)
                continue
            }
            // 每个工作台一份上下文，各自 remember 的值互不相干
            val context = StorageWorkbenchContext(workbenchId, menu, screen, requestClose)
            val id = workbenchId.toString()
            val size = workbench.size(context)
            if (size.width > StorageWorkbenches.MAX_WIDTH || size.height > StorageWorkbenches.MAX_HEIGHT)
                logger.warn(
                    "Storage workbench {} is {}x{}, larger than {}x{}; it is clipped", id, size.width, size.height,
                    StorageWorkbenches.MAX_WIDTH, StorageWorkbenches.MAX_HEIGHT,
                )
            width = maxOf(width, size.width.coerceAtMost(StorageWorkbenches.MAX_WIDTH))
            height = maxOf(height, size.height.coerceAtMost(StorageWorkbenches.MAX_HEIGHT))
            mounted[id] = Mounted(workbench, context, UiBinding(workbench.snapshot(context)))
            described += WorkbenchInfo(
                id,
                workbench.title(context).string,
                menu.workbenchWidget(workbenchId) is ClearableWidget
            )
        }
        info = described
        contentWidth = width
        contentHeight = height
    }

    val isEmpty
        get() = mounted.isEmpty()

    /** 要显示的工作台：玩家选的那个；它不在这个界面上时用第一个 */
    fun resolve(id: String): String = if (id in mounted) id else mounted.keys.firstOrNull().orEmpty()

    /** 游戏线程：每刻与每个输入事件之后调用，先处理各工作台送来的动作，再取新快照 */
    fun update() {
        if (closed) return
        for (workbench in mounted.values) workbench.update()
    }

    override fun close() {
        if (closed) return
        closed = true
        for (workbench in mounted.values) workbench.binding.close()
    }

    /** 工作台 [id] 放在下拉框一行右端的按钮 */
    @Composable
    fun Actions(id: String) {
        key(id) { mounted[id]?.ShowActions() }
    }

    /** 工作台 [id] 的内容 */
    @Composable
    fun Content(id: String) {
        key(id) { mounted[id]?.ShowContent() }
    }

    private inner class Mounted<S, A>(
        val workbench: StorageWorkbench<S, A>,
        val context: StorageWorkbenchContext,
        val binding: UiBinding<S, A>,
    ) {
        private val scope = object : StorageWorkbenchScope<A> {
            override val slots
                get() = this@StorageWorkbenchHost.slots

            override fun send(action: A) = binding.send(action)

            override fun requestClose() = this@StorageWorkbenchHost.requestClose()
        }

        fun update() {
            binding.drainActions { workbench.handle(context, it) }
            binding.update(workbench.snapshot(context))
        }

        @Composable
        fun ShowActions() = workbench.Actions(binding.value, scope)

        @Composable
        fun ShowContent() = workbench.Content(binding.value, scope)
    }

    private companion object {
        val logger = LogUtils.getLogger()
    }
}

/** 玩家选的工作台与工作区是否展开：所有存储界面共用，写进客户端配置 */
object StorageWorkbenchChoice {
    @JvmStatic
    fun select(id: String) {
        if (id == ClientConfigRuntime.storageWorkbench) return
        ClientConfigRuntime.storageWorkbench = id
        Config.INSTANCE.clientConfig.STORAGE_WORKBENCH.set(id)
        Config.INSTANCE.clientConfig.STORAGE_WORKBENCH.save()
    }

    @JvmStatic
    fun show(shown: Boolean) {
        if (shown == ClientConfigRuntime.storageWorkbenchShown) return
        ClientConfigRuntime.storageWorkbenchShown = shown
        Config.INSTANCE.clientConfig.STORAGE_WORKBENCH_SHOWN.set(shown)
        Config.INSTANCE.clientConfig.STORAGE_WORKBENCH_SHOWN.save()
    }

    /** 配方查看器把配方填进某个工作台后调用：换到它并展开工作区，让玩家看到填好的输入 */
    @JvmStatic
    fun reveal(workbench: ResourceLocation) {
        select(workbench.toString())
        show(true)
    }
}
