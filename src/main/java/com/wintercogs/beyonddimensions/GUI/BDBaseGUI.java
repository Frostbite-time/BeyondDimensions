package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.CallSeverClickPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;


// 更改渲染以及点击事件，以适配StoredStackSlot
public abstract class BDBaseGUI<T extends BDBaseMenu> extends AbstractContainerScreen<T>
{

    // 用于 shift双击加左键的效果
    ItemStack lastInvClickedStack = ItemStack.EMPTY;
    int lastInvClickedSlot = -1;
    int cleanHold = 10; // 给予半秒时间

    public BDBaseGUI(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if(this.hoveredSlot instanceof StoredStackSlot sSlot)
            {
                IStackType stack = sSlot.getStack();
                stack.renderTooltip(guiGraphics,minecraft.font,mouseX,mouseY);
            }
            else
            {
                ItemStack itemstack = this.hoveredSlot.getItem();
                guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot)
    {
        if(slot instanceof StoredStackSlot sSlot)
        {
            // 获取stack
            int x = slot.x;
            int y = slot.y;
            IStackType stack = sSlot.getStack();

            if(stack != null)
            {
                stack.render(guiGraphics,x,y);
            }

        }
        else
        {
            super.renderSlot(guiGraphics,slot);
        }
    }


    @Override
    protected void containerTick()
    {
        super.containerTick();

        if(cleanHold > 0)
        {
            cleanHold--;
        }
        else
        {
            lastInvClickedStack = ItemStack.EMPTY;
            lastInvClickedSlot = -1;
            cleanHold = 10;
        }

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        Slot slot = this.findSlot(mouseX, mouseY);
        if(!(slot instanceof StoredStackSlot))
            super.mouseDragged(mouseX,mouseY,button,dragX,dragY);

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX,mouseY,button);

        return true;
    }

    @Override
    protected void slotClicked(Slot slot, int slotIndex, int mouseButton, ClickType type)
    {
        if(!(slot instanceof StoredStackSlot))
            super.slotClicked(slot, slotIndex, mouseButton, type);


        // 十分奇怪 为什么我以前不用slotClicked而是使用了mouseClicked手动处理
        // 我记得我明明考虑过的
        if(slot != null)
        {
            if (!menu.isHanding)
            {
                int slotId = slot.index;
                IStackType clickItem;
                if(hasShiftDown())
                {
                    if(slot instanceof StoredStackSlot sSlot)
                    {
                        clickItem = sSlot.getVanillaActualStack();
                    }
                    else
                    {
                        clickItem = new ItemStackType(slot.getItem());

                        if(lastInvClickedSlot == slotId && !lastInvClickedStack.isEmpty())
                        {
                            for(Slot invSlot : menu.slots)
                            {
                                if(menu.inventoryStartIndex<=invSlot.index&& invSlot.index<menu.inventoryEndIndex)
                                {
                                    if(ItemStack.isSameItemSameComponents(lastInvClickedStack, invSlot.getItem()))
                                        PacketDistributor.sendToServer(new CallSeverClickPacket(invSlot.index,new ItemStackType(invSlot.getItem()),0,true));
                                }
                            }
                            menu.isHanding = true;
                        }
                        else if(menu.inventoryStartIndex<=slotId&& slotId<menu.inventoryEndIndex)
                        {
                            lastInvClickedStack = slot.getItem();
                            lastInvClickedSlot = slotId;
                        }

                    }
                    menu.isHanding = true;
                    PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,mouseButton,true));
                }
                else
                {
                    if(slot instanceof StoredStackSlot sSlot)
                    {
                        if(sSlot.isFake())
                        {
                            // 对于标记槽位
                            clickItem = sSlot.getVanillaActualStack();
                            menu.isHanding = true;
                            PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,mouseButton,false));
                        }
                        else
                        {
                            clickItem = sSlot.getVanillaActualStack();
                            menu.isHanding = true;
                            PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,mouseButton,false));
                        }
                    }
                }
            }
        }
        // 至此


    }


    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode)
    {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {

            if(hoveredSlot instanceof StoredStackSlot sSlot)
            {

            }
            else
            {
                // 副手交换仅对于非存储槽才生效
                if (this.minecraft.options.keySwapOffhand.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                    return true;
                }
                for(int i = 0; i < 9; ++i) {
                    if (this.minecraft.options.keyHotbarSlots[i].isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
                        this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public Font getFont() {
        return font;
    }

}
