package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer.CallSeverClickPacket;
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        Slot slot = this.findSlot(mouseX, mouseY);
        if(!(slot instanceof StoredStackSlot))
            super.mouseDragged(mouseX,mouseY,button,dragX,dragY);

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX,mouseY,button);

        // 处理点击槽位
        Slot slot = findSlot(mouseX,mouseY);
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
                    }
                    menu.isHanding = true;
                    PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,button,true));
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
                            PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,button,false));
                        }
                        else
                        {
                            clickItem = sSlot.getVanillaActualStack();
                            menu.isHanding = true;
                            PacketDistributor.sendToServer(new CallSeverClickPacket(slotId,clickItem,button,false));
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type)
    {
        if(!(slot instanceof StoredStackSlot))
            super.slotClicked(slot, slotId, mouseButton, type);
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
