package com.wintercogs.beyonddimensions.client.ui.base

import com.mojang.blaze3d.platform.InputConstants
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.client.ui.kit.formatCompact
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import com.wintercogs.beyonddimensions.common.menu.interaction.SlotClick
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.MenuSlotVisual
import dev.compixel.forge.slots.VanillaMenuSlotAdapter
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW

/**
 * BD 菜单的槽位适配：在原生容器界面中显示虚拟资源，并把槽位操作转成菜单命令。
 * 除 [isResource] 外，所有方法都在游戏线程调用
 */
class BdSlotAdapter<M : BDBaseMenu>(private val menu: M) : VanillaMenuSlotAdapter(menu), AutoCloseable {
    // 菜单创建后槽位不再增减，可在任何线程读取
    private val resourceSlots: Set<Int> =
        menu.slots.filter { it is AbstractStackTypedSlot }.mapTo(HashSet()) { it.index }
    private val resourceValues = HashMap<Int, KeyAmount>()

    /**
     * 同一种资源在任何格子里都用同一个图标句柄，滚动或排序后已画好的图像可以沿用。
     * 超过上限时丢掉最久没用的
     */
    private val icons =
        object : LinkedHashMap<IStackKey<*>, ItemIcon>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IStackKey<*>, ItemIcon>) =
                size > ICON_HANDLES
        }

    /** 空手右键存储格子里的资源时调用，返回 true 表示界面打开了自己的菜单，不发送右键点击 */
    var resourceMenu: ((slotId: Int) -> Boolean)? = null

    // 上一次交给服务端的点击，用来判定双击
    private var lastSlot = -1
    private var lastButton = -1
    private var lastTime = 0L
    private var lastStack: KeyAmount? = null

    fun tick() {
        menu.commands().flushPreference()
    }

    /** 该槽位是否显示虚拟资源；可在 Compose 线程调用 */
    fun isResource(slotId: Int) = slotId in resourceSlots

    override fun visual(slot: Slot, previous: MenuSlotVisual?): MenuSlotVisual {
        if (slot !is AbstractStackTypedSlot) return super.visual(slot, previous)
        val resource = slot.stack
        val key = resource.key()
        // 标记槽只表示资源种类。按住 Shift 时存储格子不重排，被取空的资源留在原位、显示为 0
        val shown = !key.isEmpty && (slot.isFake || slot is DisorderedStackTypedSlot || resource.amount() > 0)

        if (previous != null && resourceValues[slot.index] == resource) return previous
        resourceValues[slot.index] = resource
        if (!shown) return MenuSlotVisual(icon = emptyIcon(slot), marked = slot.isFake)
        val label = if (slot.isFake) "" else formatCompact(resource.amount())
        return MenuSlotVisual(iconOf(key), amount = label, marked = slot.isFake)
    }

    private fun iconOf(key: IStackKey<*>): ItemIcon = icons.getOrPut(key) { resourceIcon(key) }

    /** 虚拟资源不参与原版的拖动分配 */
    override fun canDragTo(slot: Slot) = slot !is AbstractStackTypedSlot

    override fun tooltip(graphics: GuiGraphics, slot: Slot, x: Int, y: Int) {
        if (slot !is AbstractStackTypedSlot) return super.tooltip(graphics, slot, x, y)
        val resource = slot.stack
        if (!resource.isEmpty || slot is DisorderedStackTypedSlot && !resource.key().isEmpty) {
            resource.key().render.renderTooltip(
                graphics,
                Minecraft.getInstance().font,
                resource.key(),
                resource.amount(),
                x,
                y
            )
        }
    }

    override fun execute(slotId: Int, button: Int, type: ClickType) {
        val slot = menu.slots.getOrNull(slotId)
        when {
            slot is AbstractStackTypedSlot -> clickResource(slotId, slot, button, type)
            slot != null && (type == ClickType.QUICK_MOVE || isMoveAllClick(slotId, button, type)) ->
                clickVanilla(slotId, slot, button, type)
            // 其余原版手势（拖动、交换、丢弃、收集）仍由原生执行，但要排在已提交的菜单操作之后
            !menu.menuSync().isSendingAction() -> super.execute(slotId, button, type)
        }
    }

    private fun clickResource(slotId: Int, slot: AbstractStackTypedSlot, button: Int, type: ClickType) {
        val emptyHand = menu.carried.isEmpty
        if (type == ClickType.PICKUP && button == 1 && slot is DisorderedStackTypedSlot && emptyHand &&
            !slot.stack.isEmpty && resourceMenu?.invoke(slotId) == true
        ) return
        when (type) {
            ClickType.PICKUP, ClickType.CLONE, ClickType.QUICK_MOVE -> {
                val sentButton = if (type == ClickType.CLONE) 2 else button
                val current = slot.vanillaActualStack
                val first = firstOfDoubleClick(slotId, sentButton, current)
                // 有序槽的第二下带上第一下点到的资源：这一格此时可能已被搬空
                val ordered = !slot.isFake && slot !is DisorderedStackTypedSlot
                menu.commands().click(
                    slotId,
                    if (first != null && ordered) first else current,
                    sentButton,
                    heldModifiers(type == ClickType.QUICK_MOVE, first != null),
                )
            }
            // 虚拟资源不参与原版的拖动、交换、丢弃与收集
            else -> {}
        }
    }

    /** 空格左键点背包或快捷栏的槽位是整批转移，交给服务端的 BD 命令，不执行原版的拿起 */
    private fun isMoveAllClick(slotId: Int, button: Int, type: ClickType) =
        type == ClickType.PICKUP && button == 0 && spaceHeld() &&
                slotId >= menu.inventoryStartIndex && slotId < menu.inventoryEndIndex

    /** 普通槽位的 Shift 点击与空格左键由服务端的 BD 命令处理，不额外发送原版的点击 */
    private fun clickVanilla(slotId: Int, slot: Slot, button: Int, type: ClickType) {
        val stack = slot.item.copy()
        val current = KeyAmount(ItemStackKey(stack), stack.count.toLong())
        // 双击的第二下带上第一下点到的物品：第一下通常已经把这一格搬空
        val first = firstOfDoubleClick(slotId, button, current)
        menu.commands()
            .click(slotId, first ?: current, button, heldModifiers(type == ClickType.QUICK_MOVE, first != null))
    }

    /**
     * 按原版的标准判定双击：同一槽位、同一按键，两次按下相隔不到 250 毫秒。
     * 是第二下时返回第一下点到的资源，否则返回 null
     */
    private fun firstOfDoubleClick(slotId: Int, button: Int, clicked: KeyAmount): KeyAmount? {
        val now = Util.getMillis()
        val first = lastStack
        if (first != null && slotId == lastSlot && button == lastButton && now - lastTime < DOUBLE_CLICK_MILLIS) {
            lastStack = null
            return first
        }
        lastSlot = slotId
        lastButton = button
        lastTime = now
        lastStack = clicked
        return null
    }

    /** 随点击发给服务端的修饰键与标志 */
    private fun heldModifiers(quickMove: Boolean, doubleClick: Boolean): Int {
        var held = if (quickMove) SlotClick.SHIFT else 0
        if (Screen.hasControlDown()) held = held or SlotClick.CTRL
        if (Screen.hasAltDown()) held = held or SlotClick.ALT
        if (spaceHeld()) held = held or SlotClick.SPACE
        if (doubleClick) held = held or SlotClick.DOUBLE
        return held
    }

    private fun spaceHeld() = InputConstants.isKeyDown(Minecraft.getInstance().window.window, GLFW.GLFW_KEY_SPACE)

    override fun close() {
        icons.clear()
        resourceValues.clear()
    }

    private companion object {
        const val DOUBLE_CLICK_MILLIS = 250L

        // 与 CompixelUI 图标缓存的上限一致
        const val ICON_HANDLES = 1024
    }
}

/** 资源的图标；在游戏线程调用 */
fun resourceIcon(key: IStackKey<*>): ItemIcon =
    if (key is ItemStackKey) ItemIcon.snapshot(key.copyStack().copyWithCount(1))
    else ItemIcon.drawn(key.toString(), { graphics -> key.render.render(graphics, key, 0, 0) })
