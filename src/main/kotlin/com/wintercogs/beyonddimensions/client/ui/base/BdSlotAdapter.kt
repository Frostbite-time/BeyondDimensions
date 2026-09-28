package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.client.ui.kit.formatCompact
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import dev.composemc.forge.item.ItemIcon
import dev.composemc.forge.slots.MenuSlotVisual
import dev.composemc.forge.slots.VanillaMenuSlotAdapter
import dev.composemc.host.UiBinding
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot

/**
 * BD 菜单的槽位适配：在原生容器界面中显示虚拟资源，并把槽位操作转成菜单命令。
 *
 * 虚拟资源的数量不交给通用槽位绘制，而是通过 [amounts] 发布给界面，以 BD 的数量标签显示。
 * 所有方法都在游戏线程调用；图标回调只捕获不可变的资源键。
 */
class BdSlotAdapter<M : BDBaseMenu>(private val menu: M) : VanillaMenuSlotAdapter(menu), AutoCloseable {
    /** 虚拟资源槽的数量文字，键为槽位编号 */
    val amounts = UiBinding<Map<Int, String>, Unit>(emptyMap())

    private val labels = HashMap<Int, String>()
    private var labelsChanged = false
    private var closed = false
    private val resourceKeys = HashMap<Int, IStackKey<*>>()
    private val resourceValues = HashMap<Int, KeyAmount>()

    // 连续两次 Shift 点击同一背包槽时，把背包里同种物品全部存入网络
    private var repeatTicks = 0
    private var lastPlayerSlot = -1
    private var lastPlayerStack: KeyAmount? = null

    fun tick() {
        menu.commands().flushPreference()
        if (repeatTicks > 0 && --repeatTicks == 0) forgetRepeat()
        publish()
    }

    fun publish() {
        if (closed || !labelsChanged) return
        labelsChanged = false
        amounts.update(HashMap(labels))
    }

    override fun visual(slot: Slot, previous: MenuSlotVisual?): MenuSlotVisual {
        if (slot !is AbstractStackTypedSlot) return super.visual(slot, previous)
        val resource = slot.stack
        val key = resource.key()
        // 标记槽只表示资源种类；存储行在点击结束前可能暂时保留数量为 0 的资源
        val shown = !key.isEmpty && (slot.isFake || resource.amount() > 0)
        setLabel(slot.index, if (shown && !slot.isFake) formatCompact(resource.amount()) else null)

        if (previous != null && resourceValues[slot.index] == resource) return previous
        resourceValues[slot.index] = resource
        if (!shown) {
            resourceKeys.remove(slot.index)
            return MenuSlotVisual(icon = emptyIcon(slot), marked = slot.isFake)
        }
        val icon =
            if (resourceKeys[slot.index] == key && previous?.icon != null) previous.icon
            else {
                resourceKeys[slot.index] = key
                iconOf(key)
            }
        return MenuSlotVisual(icon, marked = slot.isFake)
    }

    private fun setLabel(slotIndex: Int, label: String?) {
        val old = if (label == null) labels.remove(slotIndex) else labels.put(slotIndex, label)
        if (old != label) labelsChanged = true
    }

    private fun iconOf(key: IStackKey<*>): ItemIcon =
        if (key is ItemStackKey) ItemIcon.snapshot(key.copyStack().copyWithCount(1))
        else ItemIcon.drawn(key.toString(), { graphics -> key.render.render(graphics, key, 0, 0) })

    /** 虚拟资源不参与原版的拖动分配 */
    override fun canDragTo(slot: Slot) = slot !is AbstractStackTypedSlot

    override fun tooltip(graphics: GuiGraphics, slot: Slot, x: Int, y: Int) {
        if (slot !is AbstractStackTypedSlot) return super.tooltip(graphics, slot, x, y)
        val resource = slot.stack
        if (!resource.isEmpty) {
            resource.key().render.renderTooltip(graphics, Minecraft.getInstance().font, resource.key(), resource.amount(), x, y)
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
        when (type) {
            ClickType.PICKUP, ClickType.CLONE, ClickType.QUICK_MOVE ->
                menu.commands().click(
                    slotId,
                    slot.vanillaActualStack,
                    if (type == ClickType.CLONE) 2 else button,
                    type == ClickType.QUICK_MOVE,
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
        val queued = menu.commands().click(slotId, clicked, button, true)
        if (queued && !stack.isEmpty && slotId >= menu.inventoryStartIndex && slotId < menu.inventoryEndIndex) {
            lastPlayerSlot = slotId
            lastPlayerStack = clicked
            repeatTicks = REPEAT_WINDOW_TICKS
        } else if (!queued) {
            forgetRepeat()
        }
    }

    private fun forgetRepeat() {
        lastPlayerSlot = -1
        lastPlayerStack = null
        repeatTicks = 0
    }

    override fun close() {
        closed = true
        amounts.close()
        labels.clear()
        resourceKeys.clear()
        resourceValues.clear()
    }

    private companion object {
        const val REPEAT_WINDOW_TICKS = 10
    }
}
