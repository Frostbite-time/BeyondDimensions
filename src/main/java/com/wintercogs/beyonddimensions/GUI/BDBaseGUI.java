package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Packet.BatchTransferPacket;
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
import org.anti_ad.mc.ipn.api.IPNIgnore;


// 更改渲染以及点击事件，以适配StoredStackSlot
@IPNIgnore
public abstract class BDBaseGUI<T extends BDBaseMenu> extends AbstractContainerScreen<T>
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
                KeyAmount stack = sSlot.getStack();
                stack.key().getRender().renderTooltip(guiGraphics,minecraft.font,stack.key(),stack.amount(),mouseX,mouseY);
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
        if(slot instanceof AbstractStackTypedSlot sSlot)
        {
            // 获取stack
            int x = slot.x;
            int y = slot.y;
            KeyAmount stack = sSlot.getStack();

            if(stack.key().isEmpty()) return; // 不绘制空键
            stack.key().getRender().render(guiGraphics, stack.key(), x, y);
            stack.key().getRender().renderAmount(guiGraphics,stack.amount(),x,y);

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
            lastStorageClickedStack = ItemStackKey.EMPTY;
            lastInvClickedSlot = -1;
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
        if(slot != null)
        {
            // 数据伪造以及重复修改已经在服务端阻止
            // 将menu.isHanding检测暂时移除，增强原版兼容性
            // 如果一段时间后没有问题则移除
            if (true)
            {
                int slotId = slot.index;
                KeyAmount clickItem;
                if(hasShiftDown())
                {
                    if(slot instanceof AbstractStackTypedSlot sSlot)
                    {
                        clickItem = sSlot.getVanillaActualStack();
                        if(!lastStorageClickedStack.isEmpty() && lastStorageClickedStack.equals(clickItem.key()))
                        {
                            PacketDistributor.sendToServer(new BatchTransferPacket(clickItem,false));
                        }
                        else if(!clickItem.isEmpty() && clickItem.key() instanceof ItemStackKey itemStackKey)
                        {
                            this.lastStorageClickedStack = itemStackKey;
                        }
                    }
                    else
                    {
                        clickItem = new KeyAmount(new ItemStackKey(slot.getItem()),slot.getItem().getCount());

                        // 快速移动仓库物品
                        // 原版会处理一部分快速移动 此处处理原版未能正常处理的部分
                        // 理论上说，这俩者即使同时操作一个槽位也不会导致物品复制等bug
                        // 因为操作基本全由服务端处理
                        if(lastInvClickedSlot == slotId && !lastInvClickedStack.isEmpty())
                        {
                            PacketDistributor.sendToServer(new BatchTransferPacket(new KeyAmount(new ItemStackKey(lastInvClickedStack),lastInvClickedStack.getCount()) ,true));
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
                    if(slot instanceof AbstractStackTypedSlot sSlot)
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
