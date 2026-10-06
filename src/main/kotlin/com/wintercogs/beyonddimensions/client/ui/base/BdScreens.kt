package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdDesign
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.ComposeInventoryScreen
import dev.compixel.forge.ComposeMenuScreen
import dev.compixel.forge.item.NativeItemOptions
import dev.compixel.forge.slots.ComposeMenuSlots
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import org.lwjgl.glfw.GLFW

/** 界面文字在游戏线程取出，随快照或构造参数进入界面 */
fun tr(key: String, vararg args: Any): String = Component.translatable(key, *args).string

/**
 * 带原版槽位的 BD 菜单界面：用 [BdSlotAdapter] 显示虚拟资源，套用 BD 的设计与主题。
 * 子类照常实现 snapshot、handle 与 Content；JEI 与 EMI 的联动按这个类识别 BD 界面。
 */
abstract class BdInventoryScreen<M : BDBaseMenu, S, A>(menu: M, title: Component) :
    ComposeInventoryScreen<M, S, A>(
        menu,
        title,
        ComposeMenuSlots(menu, BdSlotAdapter(menu)),
        Bd.ThemeId,
        NativeItemOptions(cacheCapacity = 512),
        design = BdDesign,
    ) {
    @Suppress("UNCHECKED_CAST")
    private val adapter = inventory.adapter as BdSlotAdapter<M>

    override fun inventoryTick() = adapter.tick()

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
        design = BdDesign,
    )
