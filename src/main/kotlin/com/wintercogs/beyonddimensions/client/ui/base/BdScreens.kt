package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.client.ui.theme.BdDesign
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import dev.compixel.forge.ComposeInventoryScreen
import dev.compixel.forge.ComposeMenuScreen
import dev.compixel.forge.item.NativeItemOptions
import dev.compixel.forge.slots.ComposeMenuSlots
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.AbstractContainerMenu
import org.lwjgl.glfw.GLFW

/** 取出界面翻译文字 */
fun tr(key: String, vararg args: Any): String = Component.translatable(key, *args).string

// 一屏的图标一帧画完
private const val ICONS_PER_FRAME = 512

/** 带原版槽位的 BD 菜单界面，用 [BdSlotAdapter] 显示虚拟资源。JEI 与 EMI 的联动按这个类识别 BD 界面 */
abstract class BdInventoryScreen<M : BDBaseMenu>(menu: M, title: Component) :
    ComposeInventoryScreen<M>(
        menu,
        title,
        ComposeMenuSlots(menu, BdSlotAdapter(menu)),
        NativeItemOptions(cacheCapacity = 512, preparationsPerFrame = ICONS_PER_FRAME),
        design = BdDesign,
    ) {
    @Suppress("UNCHECKED_CAST")
    protected val adapter = inventory.adapter as BdSlotAdapter<M>

    /** 附属模组注入这个界面的页 */
    protected val pages = BdPageHost(container, this, inventory, gameThread, ::requestClose)

    override fun inventoryTick() {
        adapter.tick()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val editing = hasTextInputFocus
        val handled = super.keyPressed(keyCode, scanCode, modifiers)
        // 编辑文字时占用可打印按键，避免配方查看器把 R、U 等当作快捷键
        return handled || (editing && keyCode in GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_GRAVE_ACCENT)
    }

    override fun menuClosed() {
        adapter.close()
    }
}

/** 没有槽位的 BD 菜单界面 */
abstract class BdMenuScreen<M : AbstractContainerMenu>(menu: M, title: Component) :
    ComposeMenuScreen<M>(
        menu,
        title,
        nativeItemOptions = NativeItemOptions(cacheCapacity = 256, preparationsPerFrame = ICONS_PER_FRAME),
        design = BdDesign,
    ) {
    /** 附属模组注入这个界面的页 */
    protected val pages = BdPageHost(container, this, null, gameThread, ::requestClose)
}
