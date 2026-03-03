package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Packet.BatchTransferPacket;
import com.wintercogs.beyonddimensions.Packet.CallSeverClickPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.NotNull;


// 更改渲染以及点击事件，以适配StoredStackSlot
@IPNIgnore
public abstract class BDBaseGUI<T extends BDBaseMenu> extends AbstractContainerScreen<@NotNull T>
{

    // 用于 shift双击加左键的效果
    ItemStack lastInvClickedStack = ItemStack.EMPTY;
    ItemStackKey lastStorageClickedStack = ItemStackKey.EMPTY;
    int lastInvClickedSlot = -1;
    int cleanHold = 10; // 给予半秒时间

    public BDBaseGUI(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem())
        {
            if (this.hoveredSlot instanceof AbstractStackTypedSlot sSlot)
            {
                KeyAmount stack = sSlot.getStack();
                stack.key().getRender().renderTooltip(guiGraphics, minecraft.font, stack.key(), stack.amount(), mouseX, mouseY);
            }
            else
            {
                ItemStack itemstack = this.hoveredSlot.getItem();
                guiGraphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, mouseX, mouseY, itemstack.get(DataComponents.TOOLTIP_STYLE));
            }
        }
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot, int mouseX, int mouseY)
    {
        if (slot instanceof AbstractStackTypedSlot sSlot)
        {
            // 获取stack
            int x = slot.x;
            int y = slot.y;
            KeyAmount stack = sSlot.getStack();

            if (stack.key().isEmpty())
            {
                var noItemIcon = slot.getNoItemIcon();
                if (noItemIcon != null)
                {
                    TextureAtlasSprite textureatlassprite = this.minecraft.getTextureAtlas(noItemIcon.getFirst()).apply(noItemIcon.getSecond());
                    guiGraphics.blit(x, y, 0, 16, 16, textureatlassprite);
                }
                return;
            }
            stack.key().getRender().render(guiGraphics, stack.key(), x, y);
            stack.key().getRender().renderAmount(guiGraphics, stack.amount(), x, y);

        }
        else
        {
            super.renderSlot(guiGraphics, slot, mouseX, mouseY);
        }
    }


    @Override
    protected void containerTick()
    {
        super.containerTick();

        if (cleanHold > 0)
        {
            cleanHold--;
        }
        else
        {
            lastInvClickedStack = ItemStack.EMPTY;
            lastStorageClickedStack = ItemStackKey.EMPTY;
            lastInvClickedSlot = -1;
            cleanHold = 10;
        }

    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY)
    {
        // 先把事件交给当前 focused 控件
        GuiEventListener focused = this.getFocused();
        if (focused != null && this.isDragging())
        {
            if (focused.mouseDragged(event, dragX, dragY))
            {
                return true;
            }
            // 返回 false，继续往下走，让容器/槽逻辑按需处理
        }

        // 命中自定义槽位：拦截容器 quick-craft，不让容器接管
        Slot slot = this.getHoveredSlot(event.x(), event.y());
        if (slot instanceof AbstractStackTypedSlot) return true;

        // 其它情况：让容器逻辑处理
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event)
    {
        // AbstractContainerScreen的drag没有调用组件drag，但是Release却调用了，不需要手动重复处理
        // 在此注释，防止我某一天忘记了
        return super.mouseReleased(event);
    }

    @Override
    protected void slotClicked(Slot slot, int slotIndex, int mouseButton, ClickType type)
    {
        if (!(slot instanceof AbstractStackTypedSlot))
            super.slotClicked(slot, slotIndex, mouseButton, type);


        if (slot == null) return; // slot绝对可能为null，不可移除此行

        int slotId = slot.index;
        KeyAmount clickItem;
        if (Minecraft.getInstance().hasShiftDown())
        {
            if (slot instanceof AbstractStackTypedSlot sSlot)
            {
                clickItem = sSlot.getVanillaActualStack();
                if (!lastStorageClickedStack.isEmpty() && lastStorageClickedStack.equals(clickItem.key()))
                {
                    ClientPacketDistributor.sendToServer(new BatchTransferPacket(clickItem, false));
                }
                else if (!clickItem.isEmpty() && clickItem.key() instanceof ItemStackKey itemStackKey)
                {
                    this.lastStorageClickedStack = itemStackKey;
                }
            }
            else
            {
                clickItem = new KeyAmount(new ItemStackKey(slot.getItem()), slot.getItem().getCount());

                // 快速移动仓库物品
                // 原版会处理一部分快速移动 此处处理原版未能正常处理的部分
                // 理论上说，这俩者即使同时操作一个槽位也不会导致物品复制等bug
                // 因为操作基本全由服务端处理
                if (lastInvClickedSlot == slotId && !lastInvClickedStack.isEmpty())
                {
                    ClientPacketDistributor.sendToServer(new BatchTransferPacket(new KeyAmount(new ItemStackKey(lastInvClickedStack), lastInvClickedStack.getCount()), true));
                }
                else if (menu.inventoryStartIndex <= slotId && slotId < menu.inventoryEndIndex)
                {
                    lastInvClickedStack = slot.getItem();
                    lastInvClickedSlot = slotId;
                }

            }
            ClientPacketDistributor.sendToServer(new CallSeverClickPacket(slotId, clickItem, mouseButton, true));
        }
        else
        {
            if (slot instanceof AbstractStackTypedSlot sSlot)
            {
                if (sSlot.isFake())
                {
                    // 对于标记槽位
                    clickItem = sSlot.getVanillaActualStack();
                    ClientPacketDistributor.sendToServer(new CallSeverClickPacket(slotId, clickItem, mouseButton, false));
                }
                else
                {
                    clickItem = sSlot.getVanillaActualStack();
                    ClientPacketDistributor.sendToServer(new CallSeverClickPacket(slotId, clickItem, mouseButton, false));
                }
            }
        }

    }

    @Override
    protected boolean checkHotbarKeyPressed(@NotNull KeyEvent event)
    {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null)
        {

            if (hoveredSlot instanceof AbstractStackTypedSlot sSlot)
            {

            }
            else
            {
                // 副手交换仅对于非存储槽才生效
                if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(InputConstants.getKey(event)))
                {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                    return true;
                }
                for (int i = 0; i < 9; ++i)
                {
                    if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(InputConstants.getKey(event)))
                    {
                        this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public @NotNull Font getFont()
    {
        return font;
    }

}
