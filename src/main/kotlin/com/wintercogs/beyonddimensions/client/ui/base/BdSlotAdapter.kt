package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.client.ui.kit.formatCompact
import com.mojang.blaze3d.platform.InputConstants
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import com.wintercogs.beyonddimensions.common.menu.interaction.SlotClick
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.MenuSlotVisual
import dev.compixel.forge.slots.VanillaMenuSlotAdapter
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW

/**
 * BD 菜单的槽位适配：在原生容器界面中显示虚拟资源，并把槽位操作转成菜单命令。
 *
 * 虚拟资源的数量随槽位的显示内容一起交给界面，由 [BdSlot] 以 BD 的数量标签显示。
 * 除 [isResource] 外，所有方法都在游戏线程调用；图标回调只捕获不可变的资源键。
 */
class BdSlotAdapter<M : BDBaseMenu>(private val menu: M) : VanillaMenuSlotAdapter(menu), AutoCloseable {
    // 菜单创建后槽位不再增减，可在任何线程读取
    private val resourceSlots: Set<Int> =
        menu.slots.filter { it is AbstractStackTypedSlot }.mapTo(HashSet()) { it.index }
    private val resourceValues = HashMap<Int, KeyAmount>()

    /**
     * 同一种资源在任何格子里都用同一个图标句柄。图集按句柄认图：滚动或排序让资源换了格子时，已经画好的图像可以直接沿用；
     * 否则大格子区滚动一行就有上百个新句柄，超出每帧能准备的数量，没画好的格子会成片显示成暗色方块。
     * 最近用过的在前，超过上限时丢掉最久没用的，图集自己的缓存也有上限
     */
    private val icons =
        object : LinkedHashMap<IStackKey<*>, ItemIcon>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IStackKey<*>, ItemIcon>) =
                size > ICON_HANDLES
        }

    /** 空手右键存储格子里的资源时调用，返回 true 表示界面打开了自己的菜单，不再发送右键点击 */
    var resourceMenu: ((slotId: Int) -> Boolean)? = null

    // 连续两次 Shift 点击同一背包槽时，把背包里同种物品全部存入网络
    private var repeatTicks = 0
    private var lastPlayerSlot = -1
    private var lastPlayerStack: KeyAmount? = null

    fun tick() {
        menu.commands().flushPreference()
        if (repeatTicks > 0 && --repeatTicks == 0) forgetRepeat()
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
            slot != null && type == ClickType.QUICK_MOVE -> quickMove(slotId, slot, button)
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
            ClickType.PICKUP, ClickType.CLONE, ClickType.QUICK_MOVE ->
                menu.commands().click(
                    slotId,
                    slot.vanillaActualStack,
                    if (type == ClickType.CLONE) 2 else button,
                    heldModifiers(type == ClickType.QUICK_MOVE),
                )
            // 虚拟资源不参与原版的拖动、交换、丢弃与收集
            else -> {}
        }
    }

    /** 普通槽位的 Shift 点击由服务端的 BD 命令处理，不再额外发送原版的快速移动 */
    private fun quickMove(slotId: Int, slot: Slot, button: Int) {
        val stack = slot.item.copy()
        val repeated = lastPlayerStack
        if (lastPlayerSlot == slotId && repeated != null && repeatTicks > 0) {
            menu.commands().batch(repeated, true)
            forgetRepeat()
            return
        }
        val clicked = KeyAmount(ItemStackKey(stack), stack.count.toLong())
        val queued = menu.commands().click(slotId, clicked, button, heldModifiers(quickMove = true))
        if (queued && !stack.isEmpty && slotId >= menu.inventoryStartIndex && slotId < menu.inventoryEndIndex) {
            lastPlayerSlot = slotId
            lastPlayerStack = clicked
            repeatTicks = REPEAT_WINDOW_TICKS
        } else if (!queued) {
            forgetRepeat()
        }
    }

    /** 随点击发给服务端的修饰键：Shift 以点击类型为准，Ctrl（macOS 上为 Command）、Alt 与空格读当前键盘 */
    private fun heldModifiers(quickMove: Boolean): Int {
        var held = if (quickMove) SlotClick.SHIFT else 0
        if (Screen.hasControlDown()) held = held or SlotClick.CTRL
        if (Screen.hasAltDown()) held = held or SlotClick.ALT
        if (InputConstants.isKeyDown(Minecraft.getInstance().window.window, GLFW.GLFW_KEY_SPACE)) held = held or SlotClick.SPACE
        return held
    }

    private fun forgetRepeat() {
        lastPlayerSlot = -1
        lastPlayerStack = null
        repeatTicks = 0
    }

    override fun close() {
        icons.clear()
        resourceValues.clear()
    }

    private companion object {
        const val REPEAT_WINDOW_TICKS = 10

        // 与 CompixelUI 图标缓存的上限一致
        const val ICON_HANDLES = 1024
    }
}

/** 资源的图标：物品取快照，其他资源按自己的渲染方式绘制。在游戏线程调用 */
fun resourceIcon(key: IStackKey<*>): ItemIcon =
    if (key is ItemStackKey) ItemIcon.snapshot(key.copyStack().copyWithCount(1))
    else ItemIcon.drawn(key.toString(), { graphics -> key.render.render(graphics, key, 0, 0) })
