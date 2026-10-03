package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.ComposeInventoryScreen
import dev.compixel.forge.ComposeMenuScreen
import dev.compixel.forge.item.NativeItemOptions
import dev.compixel.forge.slots.ComposeMenuSlots
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import org.lwjgl.glfw.GLFW

/**
 * 页面控制器：在游戏线程读取菜单生成不可变快照，并处理界面发回的操作。
 * 多个界面共用的控制器由界面的 snapshot 与 handle 转调；绑定与生命周期由 CompixelUI 管理。
 */
interface BdController<S, A> {
    fun snapshot(): S

    fun handle(action: A)
}

/** 界面内容可用的界面操作，例如标题栏的关闭按钮 */
class BdScreenScope internal constructor(private val onClose: () -> Unit) {
    fun close() = onClose()
}

val LocalBdScreen = staticCompositionLocalOf<BdScreenScope> { error("Not inside a BD screen") }

/** 界面文字在游戏线程取出，随快照或构造参数进入界面 */
fun tr(key: String, vararg args: Any): String = Component.translatable(key, *args).string

/**
 * 带原版槽位的 BD 菜单界面。保留原生容器的输入与渲染钩子；快照、操作与关闭请求由 CompixelUI 按会话管理，
 * 临时进入配方查看器再返回时，界面以新的快照重新开始。
 */
abstract class BdInventoryScreen<M : BDBaseMenu, S, A>(menu: M, title: Component) :
    ComposeInventoryScreen<M, S, A>(
        menu,
        title,
        ComposeMenuSlots(menu, BdSlotAdapter(menu)),
        Bd.ThemeId,
        NativeItemOptions(cacheCapacity = 512),
    ) {
    @Suppress("UNCHECKED_CAST") private val adapter = inventory.adapter as BdSlotAdapter<M>
    private val scope = BdScreenScope(::requestClose)

    /** 页面内容：只读取 [state] 与 [slots]，操作经 [send] 发回 */
    @Composable protected abstract fun Page(state: S, slots: ComposeMenuSlots<M>)

    @Composable
    final override fun Content(state: S, slots: ComposeMenuSlots<M>) {
        CompositionLocalProvider(LocalBdScreen provides scope, LocalSlotAmounts provides adapter.amounts) {
            Page(state, slots)
        }
    }

    override fun inventoryTick() = adapter.tick()

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        adapter.publish()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val editing = hasTextInputFocus
        val handled = super.keyPressed(keyCode, scanCode, modifiers)
        // 文字由 charTyped 单独送达；编辑时占用可打印按键，避免配方查看器把 R、U 等当作快捷键
        return handled || (editing && keyCode in GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_GRAVE_ACCENT)
    }

    override fun menuClosed() = adapter.close()
}

/** 没有槽位的 BD 菜单界面 */
abstract class BdMenuScreen<M : AbstractContainerMenu, S, A>(menu: M, title: Component) :
    ComposeMenuScreen<M, S, A>(
        menu,
        title,
        theme = Bd.ThemeId,
        nativeItemOptions = NativeItemOptions(cacheCapacity = 256),
    ) {
    private val scope = BdScreenScope(::requestClose)

    /** 页面内容：只读取 [state]，操作经 [send] 发回 */
    @Composable protected abstract fun Page(state: S)

    @Composable
    final override fun Content(state: S) {
        CompositionLocalProvider(LocalBdScreen provides scope) { Page(state) }
    }
}
