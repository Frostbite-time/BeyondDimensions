package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot
import com.wintercogs.beyonddimensions.network.packet.c2s.BatchTransferPacket
import com.wintercogs.beyonddimensions.network.packet.c2s.CallSeverClickPacket
import dev.compixel.forge.item.ItemIcon
import dev.compixel.forge.slots.MenuSlotVisual
import dev.compixel.forge.slots.VanillaMenuSlotAdapter
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor

/**
 * BD 菜单的槽位适配，行为与旧版界面一致：虚拟资源槽显示资源与数量，点击通过原有的数据包交给服务端；
 * 普通槽位仍走原版逻辑。所有方法都在游戏线程调用。
 */
open class BdSlotAdapter<M : BDBaseMenu>(protected val menu: M) : VanillaMenuSlotAdapter(menu) {
    private val resourceKeys = HashMap<Int, IStackKey<*>>()
    private val resources = HashMap<Int, KeyAmount>()

    // Shift 连按同一背包槽时把背包里同种物品全部存入网络（与旧版相同）
    private var lastInvClickedStack: ItemStack = ItemStack.EMPTY
    private var lastInvClickedSlot = -1
    private var cleanHold = CLEAN_HOLD_TICKS

    fun tick() {
        if (cleanHold > 0) cleanHold--
        else {
            lastInvClickedStack = ItemStack.EMPTY
            lastInvClickedSlot = -1
            cleanHold = CLEAN_HOLD_TICKS
        }
    }

    override fun visual(slot: Slot, previous: MenuSlotVisual?): MenuSlotVisual {
        if (slot !is AbstractStackTypedSlot) return super.visual(slot, previous)
        val resource = slot.stack
        val key = resource.key()
        if (key.isEmpty) {
            resourceKeys.remove(slot.index)
            resources.remove(slot.index)
            // 与旧版相同：槽位自己的空槽图标（如盔甲轮廓）优先，其余标记槽显示过滤标记
            return MenuSlotVisual(icon = emptyIcon(slot) ?: if (slot.isFake) BdIcons.filterMarker else null, marked = slot.isFake)
        }
        if (previous != null && resources[slot.index] == resource) return previous
        resources[slot.index] = resource
        val amount = key.render.getCountText(resource.amount())
        val icon =
            if (resourceKeys[slot.index] == key && previous?.icon != null) previous.icon
            else {
                resourceKeys[slot.index] = key
                iconOf(key)
            }
        return MenuSlotVisual(icon, amount, marked = slot.isFake)
    }

    private fun iconOf(key: IStackKey<*>): ItemIcon =
        if (key is ItemStackKey) ItemIcon.snapshot(key.copyStack().copyWithCount(1))
        else ItemIcon.drawn(key.toString(), { graphics -> key.render.render(graphics, key, 0, 0) })

    /** 虚拟资源不参与原版的拖动分配 */
    override fun canDragTo(slot: Slot) = slot !is AbstractStackTypedSlot

    override fun tooltip(graphics: GuiGraphics, slot: Slot, x: Int, y: Int) {
        if (slot !is AbstractStackTypedSlot) return super.tooltip(graphics, slot, x, y)
        val resource = slot.stack
        if (!resource.isEmpty)
            resource.key().render.renderTooltip(graphics, Minecraft.getInstance().font, resource.key(), resource.amount(), x, y)
    }

    override fun execute(slotId: Int, button: Int, type: ClickType) {
        val slot = menu.slots.getOrNull(slotId)
        if (slot !is AbstractStackTypedSlot) {
            // 原版手势（拖动、交换、丢弃、收集）仍由原生执行
            super.execute(slotId, button, type)
            if (slot == null || type != ClickType.QUICK_MOVE) return
            val stack = slot.item
            val clicked = KeyAmount(ItemStackKey(stack), stack.count.toLong())
            if (lastInvClickedSlot == slotId && !lastInvClickedStack.isEmpty)
                PacketDistributor.sendToServer(
                    BatchTransferPacket(KeyAmount(ItemStackKey(lastInvClickedStack), lastInvClickedStack.count.toLong()), true)
                )
            else if (slotId >= menu.inventoryStartIndex && slotId < menu.inventoryEndIndex) {
                lastInvClickedStack = stack
                lastInvClickedSlot = slotId
            }
            PacketDistributor.sendToServer(CallSeverClickPacket(slotId, clicked, button, true))
            return
        }
        when (type) {
            ClickType.QUICK_MOVE -> PacketDistributor.sendToServer(CallSeverClickPacket(slotId, slot.vanillaActualStack, button, true))
            ClickType.PICKUP -> PacketDistributor.sendToServer(CallSeverClickPacket(slotId, slot.vanillaActualStack, button, false))
            ClickType.CLONE -> PacketDistributor.sendToServer(CallSeverClickPacket(slotId, slot.vanillaActualStack, 2, false))
            // 虚拟资源不参与原版的交换、丢弃与收集
            else -> {}
        }
    }

    private companion object {
        const val CLEAN_HOLD_TICKS = 10
    }
}
