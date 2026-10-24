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

/** 界面文字在游戏线程取出，随快照或构造参数进入界面 */
fun tr(key: String, vararg args: Any): String = Component.translatable(key, *args).string

// 一屏的图标一帧画完
private const val ICONS_PER_FRAME = 512

/** 带原版槽位的 BD 菜单界面，用 [BdSlotAdapter] 显示虚拟资源。JEI 与 EMI 的联动按这个类识别 BD 界面 */
abstract class BdInventoryScreen<M : BDBaseMenu, S, A>(menu: M, title: Component) :
    ComposeInventoryScreen<M, S, A>(
        menu,
        title,
        ComposeMenuSlots(menu, BdSlotAdapter(menu)),
        NativeItemOptions(cacheCapacity = 512, preparationsPerFrame = ICONS_PER_FRAME),
        design = BdDesign,
    ) {
    @Suppress("UNCHECKED_CAST")
    protected val adapter = inventory.adapter as BdSlotAdapter<M>

    /** 附属模组注入这个界面的页 */
    protected val pages = BdPageHost(container, this, inventory, ::requestClose)

    override fun inventoryTick() {
        adapter.tick()
        pages.update()
    }

    // 注入页送出的动作随即处理，不等下一刻
    protected open fun afterInput() = pages.update()

    override fun mouseClicked(x: Double, y: Double, button: Int) =
        super.mouseClicked(x, y, button).also { afterInput() }

    override fun mouseReleased(x: Double, y: Double, button: Int) =
        super.mouseReleased(x, y, button).also { afterInput() }

    override fun mouseScrolled(x: Double, y: Double, sy: Double) =
        super.mouseScrolled(x, y, sy).also { afterInput() }

    override fun charTyped(character: Char, modifiers: Int) =
        super.charTyped(character, modifiers).also { afterInput() }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val editing = hasTextInputFocus
        val handled = super.keyPressed(keyCode, scanCode, modifiers)
        afterInput()
        // 编辑文字时占用可打印按键，避免配方查看器把 R、U 等当作快捷键
        return handled || (editing && keyCode in GLFW.GLFW_KEY_SPACE..GLFW.GLFW_KEY_GRAVE_ACCENT)
    }

    override fun menuClosed() {
        adapter.close()
        pages.close()
    }
}

/** 没有槽位的 BD 菜单界面 */
abstract class BdMenuScreen<M : AbstractContainerMenu, S, A>(menu: M, title: Component) :
    ComposeMenuScreen<M, S, A>(
        menu,
        title,
        nativeItemOptions = NativeItemOptions(cacheCapacity = 256, preparationsPerFrame = ICONS_PER_FRAME),
        design = BdDesign,
    ) {
    /** 附属模组注入这个界面的页 */
    protected val pages = BdPageHost(container, this, null, ::requestClose)

    override fun containerTick() = pages.update()

    override fun mouseClicked(x: Double, y: Double, button: Int) =
        super.mouseClicked(x, y, button).also { pages.update() }

    override fun mouseReleased(x: Double, y: Double, button: Int) =
        super.mouseReleased(x, y, button).also { pages.update() }

    override fun mouseScrolled(x: Double, y: Double, sy: Double) =
        super.mouseScrolled(x, y, sy).also { pages.update() }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) =
        super.keyPressed(keyCode, scanCode, modifiers).also { pages.update() }

    override fun charTyped(character: Char, modifiers: Int) =
        super.charTyped(character, modifiers).also { pages.update() }

    override fun menuClosed() = pages.close()
}
