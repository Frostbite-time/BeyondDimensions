package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer.CallSeverClickPacket;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.BatchTransferPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.anti_ad.mc.ipn.api.IPNIgnore;


// 更改渲染以及点击事件，以适配StoredStackSlot
@IPNIgnore
public abstract class BDBaseGUI<T extends BDBaseMenu> extends AbstractContainerScreen<T>
{

    // 用于 shift双击加左键的效果
    ItemStack lastInvClickedStack = ItemStack.EMPTY;
    ItemStackType lastStorageClickedStack = new ItemStackType();
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
            if(this.hoveredSlot instanceof AbstractStackTypedSlot sSlot)
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
    public void renderSlot(GuiGraphics guiGraphics, Slot slot)
    {
        if(slot instanceof AbstractStackTypedSlot sSlot)
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
            lastStorageClickedStack = new ItemStackType();
            cleanHold = 10;
        }

    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        Slot slot = this.findSlot(mouseX, mouseY);
        if(!(slot instanceof AbstractStackTypedSlot))
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
        if(!(slot instanceof AbstractStackTypedSlot))
            super.slotClicked(slot, slotIndex, mouseButton, type);

        // 十分奇怪 为什么我以前不用slotClicked而是使用了mouseClicked手动处理
        // 我记得我明明考虑过的
        // 我会把这段代码作为测试版推出一段时间，以防止这里潜藏着被我遗忘的虫子
        if(slot != null)
        {
            if (true)
            {
                int slotId = slot.index;
                IStackType clickItem;
                if(hasShiftDown())
                {
                    if(slot instanceof AbstractStackTypedSlot sSlot)
                    {
                        clickItem = sSlot.getVanillaActualStack();
                        if(!lastStorageClickedStack.isEmpty() && lastStorageClickedStack.equals(clickItem))
                        {
                            PacketRegister.INSTANCE.sendToServer(new BatchTransferPacket(lastStorageClickedStack.copyWithCount(Long.MAX_VALUE),false));
                        }
                        else if(!clickItem.isEmpty() && clickItem instanceof ItemStackType itemStackType)
                        {
                            this.lastStorageClickedStack = (ItemStackType)itemStackType.copy();
                        }
                    }
                    else
                    {
                        clickItem = new ItemStackType(slot.getItem());

                        if(lastInvClickedSlot == slotId && !lastInvClickedStack.isEmpty())
                        {
                            PacketRegister.INSTANCE.sendToServer(new BatchTransferPacket(new ItemStackType(lastInvClickedStack),true));
                            menu.isHanding = true;
                        }
                        else if(menu.inventoryStartIndex<=slotId&& slotId<menu.inventoryEndIndex)
                        {
                            lastInvClickedStack = slot.getItem();
                            lastInvClickedSlot = slotId;
                        }

                    }
                    menu.isHanding = true;
                    PacketRegister.INSTANCE.sendToServer(new CallSeverClickPacket(slotId,clickItem,mouseButton,true));
                }
                else
                {
                    if(slot instanceof AbstractStackTypedSlot sSlot)
                    {
                        if(sSlot.isFake())
                        {
                            // 对于标记槽位
                            clickItem = sSlot.getVanillaActualStack();
                            menu.isHanding = true;
                            PacketRegister.INSTANCE.sendToServer(new CallSeverClickPacket(slotId,clickItem,mouseButton,false));
                        }
                        else
                        {
                            clickItem = sSlot.getVanillaActualStack();
                            menu.isHanding = true;
                            PacketRegister.INSTANCE.sendToServer(new CallSeverClickPacket(slotId,clickItem,mouseButton,false));
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

            if(hoveredSlot instanceof AbstractStackTypedSlot sSlot)
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
