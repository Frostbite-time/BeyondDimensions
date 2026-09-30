package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.ComposeInventoryScreen
import dev.compixel.forge.ComposeMenuScreen
import dev.compixel.forge.item.NativeItemOptions
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import dev.compixel.ui.ore.theme.OreThemeId
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Rect2i
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

    /** 物品网格：数量标签使用浅色文字，过滤槽的外框与普通槽位相同 */
    val Grid = OreThemeId("beyonddimensions", "grid")

    /** 旧版的小图标按钮（存储界面的侧边栏、合成区的小按钮等）：灰色凸起、左上亮右下暗，外圈与底边同为灰色；悬停时变蓝 */
    val Sidebar = OreThemeId("beyonddimensions", "sidebar")

    /** 滚动条：浅色滑块与槽位色的轨道 */
    val Scroller = OreThemeId("beyonddimensions", "scroller")

    /** 设备界面两侧的模式按钮：与面板同色的凸起按钮 */
    val Tab = OreThemeId("beyonddimensions", "tab")

    /** 能量条：暗红底色上的红色填充 */
    val Energy = OreThemeId("beyonddimensions", "energy")

    /** 网络控制的成员列表：深色底与双层边框 */
    val List = OreThemeId("beyonddimensions", "list")

    /** 选中的列表项：按钮保持悬停时的颜色，如同旧版获得焦点的按钮 */
    val Selected = OreThemeId("beyonddimensions", "selected")

    /** 不可用的按钮：旧版原版按钮的黑框深灰底 */
    val Disabled = OreThemeId("beyonddimensions", "disabled")
}

/*
 * 旧版界面坐标。Ore 面板不加内边距时，内容从边框内侧 (2, 2) 开始；这里把旧版的界面坐标换算为其中的偏移。
 * 文字放在旧版的绘制坐标上时与旧版的基线对齐。逐项绝对定位，避免文字行高的小数像素在纵向排列中累积。
 */

/** 旧版界面坐标 (x, y) */
fun Modifier.at(x: Int, y: Int): Modifier = offset((x - 2).dp, (y - 2).dp)

/** 旧版槽位坐标（槽位内容的左上角）对应的槽位外框位置 */
fun Modifier.slotAt(x: Int, y: Int): Modifier = at(x - 1, y - 1)

/** 右端对齐旧版 x 坐标 [right] 的文字 */
fun Modifier.endAt(right: Int, y: Int): Modifier = at(0, y).width(right.dp).wrapContentWidth(Alignment.End)

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
    private val outside: OutsideAreas,
    content: @Composable (ComposeMenuSlots<M>) -> Unit,
) :
    ComposeInventoryScreen<M>(
        ownedMenu,
        title,
        ComposeMenuSlots(ownedMenu, adapter),
        BdThemes.Screen,
        NativeItemOptions(cacheCapacity = 512),
        { slots -> CompositionLocalProvider(LocalOutsideAreas provides outside) { content(slots) } },
    ) {
    protected constructor(
        menu: M,
        title: Component,
        controller: BdController<*, *>?,
        content: @Composable (ComposeMenuSlots<M>) -> Unit,
    ) : this(menu, title, BdSlotAdapter(menu), controller, OutsideAreas(), content)

    /** 面板外的按钮所占的界面区域，供配方查看器避让 */
    fun extraAreas(): List<Rect2i> = outside.rects(this)

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
