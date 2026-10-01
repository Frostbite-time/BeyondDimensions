package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.ComposeInventoryScreen
import dev.compixel.forge.ComposeMenuScreen
import dev.compixel.forge.item.NativeItemOptions
import dev.compixel.forge.slots.ComposeMenuSlots
import dev.compixel.host.UiBinding
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import org.lwjgl.glfw.GLFW

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

/** 与具体页面无关、由界面发往游戏线程的请求 */
enum class ScreenRequest {
    Close,
}

class BdScreenScope internal constructor(private val requests: UiBinding<Unit, ScreenRequest>) {
    fun close() {
        requests.send(ScreenRequest.Close)
    }
}

val LocalBdScreen = staticCompositionLocalOf<BdScreenScope> { error("Not inside a BD screen") }

/** 界面文字在游戏线程取出，随快照或构造参数进入界面 */
fun tr(key: String, vararg args: Any): String = Component.translatable(key, *args).string

/**
 * 带原版槽位的 BD 菜单界面。保留原生容器的输入与渲染钩子，
 * 临时进入配方查看器再返回时保留控制器与绑定，直到菜单真正关闭。
 */
abstract class BdInventoryScreen<M : BDBaseMenu>
private constructor(
    private val ownedMenu: M,
    title: Component,
    private val adapter: BdSlotAdapter<M>,
    private val requests: UiBinding<Unit, ScreenRequest>,
    private val controller: BdController<*, *>?,
    content: @Composable (ComposeMenuSlots<M>) -> Unit,
) : ComposeInventoryScreen<M>(
        ownedMenu,
        title,
        ComposeMenuSlots(ownedMenu, adapter),
        Bd.ThemeId,
        NativeItemOptions(cacheCapacity = 512),
        content = { slots ->
            val scope = remember { BdScreenScope(requests) }
            CompositionLocalProvider(LocalBdScreen provides scope, LocalSlotAmounts provides adapter.amounts) {
                content(slots)
            }
        },
    ) {
    protected constructor(
        menu: M,
        title: Component,
        controller: BdController<*, *>?,
        content: @Composable (ComposeMenuSlots<M>) -> Unit,
    ) : this(menu, title, BdSlotAdapter(menu), UiBinding(Unit), controller, content)

    override fun inventoryTick() {
        adapter.tick()
        controller?.tick()
        requests.drainActions { request ->
            when (request) {
                ScreenRequest.Close -> onClose()
            }
        }
    }

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

    override fun removed() {
        val menuStillOpen = Minecraft.getInstance().player?.containerMenu === ownedMenu
        try {
            super.removed()
        } finally {
            if (!menuStillOpen) {
                controller?.close()
                requests.close()
                adapter.close()
            }
        }
    }
}

/** 没有槽位的 BD 菜单界面 */
abstract class BdMenuScreen<M : AbstractContainerMenu>
private constructor(
    private val ownedMenu: M,
    title: Component,
    private val requests: UiBinding<Unit, ScreenRequest>,
    private val controller: BdController<*, *>,
    content: @Composable () -> Unit,
) :
    ComposeMenuScreen<M>(
        ownedMenu,
        title,
        theme = Bd.ThemeId,
        nativeItemOptions = NativeItemOptions(cacheCapacity = 256),
        content = {
            val scope = remember { BdScreenScope(requests) }
            CompositionLocalProvider(LocalBdScreen provides scope) { content() }
        },
    ) {
    protected constructor(
        menu: M,
        title: Component,
        controller: BdController<*, *>,
        content: @Composable () -> Unit,
    ) : this(menu, title, UiBinding(Unit), controller, content)

    override fun containerTick() {
        controller.tick()
        requests.drainActions { request ->
            when (request) {
                ScreenRequest.Close -> onClose()
            }
        }
    }

    override fun removed() {
        val menuStillOpen = Minecraft.getInstance().player?.containerMenu === ownedMenu
        try {
            super.removed()
        } finally {
            if (!menuStillOpen) {
                controller.close()
                requests.close()
            }
        }
    }
}
