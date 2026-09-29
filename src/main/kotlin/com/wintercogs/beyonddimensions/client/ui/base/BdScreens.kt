package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.runtime.Composable
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.ComposeInventoryScreen
import dev.compixel.forge.ComposeMenuScreen
import dev.compixel.forge.item.NativeItemOptions
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.theme.OreThemeId
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import org.lwjgl.glfw.GLFW

/**
 * BD 界面的主题。配色全部来自资源文件 assets/beyonddimensions/compixel/ore_themes/，资源包可以覆盖其中任意颜色。
 * 界面只使用 Ore 组件，不定义自己的基础组件。
 */
object BdThemes {
    /** 容器面板、槽位、按钮等的整体配色 */
    val Screen = OreThemeId("beyonddimensions", "default")

    /** 输入框：沿用原版黑底输入框的配色 */
    val Field = OreThemeId("beyonddimensions", "field")

    /** 物品网格：数量标签使用浅色文字 */
    val Grid = OreThemeId("beyonddimensions", "grid")

    /** 存储界面左侧的按钮栏：与旧版一样的凹陷按钮，悬停时变蓝 */
    val Sidebar = OreThemeId("beyonddimensions", "sidebar")

    /** 滚动条：浅色滑块与槽位色的轨道 */
    val Scroller = OreThemeId("beyonddimensions", "scroller")

    /** 设备界面两侧的模式按钮：与面板同色的凸起按钮 */
    val Tab = OreThemeId("beyonddimensions", "tab")
}

/**
 * 页面控制器：在游戏线程读取菜单并发布不可变快照，处理界面发回的操作。
 * 可组合项只读取快照，从不直接访问菜单或背包。
 */
abstract class BdController<S : Any, A : Any>(initial: S) : AutoCloseable {
    val ui = UiBinding<S, A>(initial)

    protected abstract fun snapshot(): S

    protected abstract fun handle(action: A)

    /** 先处理操作再发布快照，使界面在同一刻看到操作结果 */
    fun tick() {
        ui.drainActions(::handle)
        ui.update(snapshot())
    }

    override fun close() = ui.close()
}

/** 界面文字在游戏线程取出，随快照或构造参数进入界面 */
fun tr(key: String, vararg args: Any): String = Component.translatable(key, *args).string

/**
 * 带原版槽位的 BD 菜单界面。保留原生容器的输入与渲染钩子；
 * 临时进入配方查看器再返回时保留控制器与绑定，直到菜单真正关闭。
 */
abstract class BdInventoryScreen<M : BDBaseMenu>
private constructor(
    private val ownedMenu: M,
    title: Component,
    protected val adapter: BdSlotAdapter<M>,
    private val controller: BdController<*, *>?,
    content: @Composable (ComposeMenuSlots<M>) -> Unit,
) :
    ComposeInventoryScreen<M>(
        ownedMenu,
        title,
        ComposeMenuSlots(ownedMenu, adapter),
        BdThemes.Screen,
        NativeItemOptions(cacheCapacity = 512),
        content,
    ) {
    protected constructor(
        menu: M,
        title: Component,
        controller: BdController<*, *>?,
        content: @Composable (ComposeMenuSlots<M>) -> Unit,
    ) : this(menu, title, BdSlotAdapter(menu), controller, content)

    override fun inventoryTick() {
        adapter.tick()
        controller?.tick()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val editing = hasTextInputFocus
        val handled = super.keyPressed(keyCode, scanCode, modifiers)
        // 文字由 charTyped 单独送达；编辑时占用可打印按键，避免配方查看器把 R、U 等当作快捷键
        return handled || (editing && keyCode in GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_GRAVE_ACCENT)
    }

    override fun removed() {
        val menuStillOpen = Minecraft.getInstance().player?.containerMenu === ownedMenu
        try {
            super.removed()
        } finally {
            if (!menuStillOpen) controller?.close()
        }
    }
}

/** 没有槽位的 BD 菜单界面 */
abstract class BdMenuScreen<M : AbstractContainerMenu>(
    private val ownedMenu: M,
    title: Component,
    private val controller: BdController<*, *>,
    content: @Composable () -> Unit,
) :
    ComposeMenuScreen<M>(
        ownedMenu,
        title,
        theme = BdThemes.Screen,
        nativeItemOptions = NativeItemOptions(cacheCapacity = 256),
        content = content,
    ) {
    override fun containerTick() = controller.tick()

    override fun removed() {
        val menuStillOpen = Minecraft.getInstance().player?.containerMenu === ownedMenu
        try {
            super.removed()
        } finally {
            if (!menuStillOpen) controller.close()
        }
    }
}
