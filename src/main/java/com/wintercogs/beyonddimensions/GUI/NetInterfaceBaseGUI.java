package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.GUI.Widget.Button.ReverseButton;
import com.wintercogs.beyonddimensions.GUI.Widget.Scroller.BigScroller;
import com.wintercogs.beyonddimensions.Integration.EMI.SlotHandler.SlotDragHandler;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.CallSeverClickPacket;
import com.wintercogs.beyonddimensions.Packet.CallSeverStoragePacket;
import com.wintercogs.beyonddimensions.Packet.FlagSlotSetPacket;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

public class NetInterfaceBaseGUI extends AbstractContainerScreen<NetInterfaceBaseMenu>
{

    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.parse("beyonddimensions:textures/gui/net_interface.png");
    private BigScroller scroller;

    public ReverseButton popButton; // 使用倒序按钮来临时替代弹出模式

    private SlotDragHandler dragHandler; // 仅在emi加载时可用

    private dev.emi.emi.api.stack.EmiIngredient dragIngredient; // 仅在emi加载时可用
    private boolean isDragging = false;


    public NetInterfaceBaseGUI(NetInterfaceBaseMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置
        this.imageWidth = 176;
        this.imageHeight = 207;
    }



    @Override
    protected void init() {
        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.leftPos = (this.width - 176)/2;
        this.topPos = (this.height - 235)/2;

        // 初始化emi dragHandler
        if(BeyondDimensions.EMILoaded)
        {
            dragHandler = new SlotDragHandler(
                slot -> {
                    if(slot instanceof StoredStackSlot sSlot)
                    {
                        return sSlot.isFake();
                    }
                    return false;
                },

                (slot, ingredient) -> {
                    Object stackKey = ingredient.getEmiStacks().get(0).getKey();
                    long stackAmount = ingredient.getEmiStacks().get(0).getAmount();
                    DataComponentPatch dataComponentPatch = ingredient.getEmiStacks().get(0).getComponentChanges();

                    IStackType dragging = new ItemStackType();
                    for(IStackType type : StackTypeRegistry.getAllTypes())
                    {
                        if(type.getSourceClass().isAssignableFrom(stackKey.getClass()))
                        {
                            //dragging = StackCreater.Create(type.getTypeId(),stackKey,1);
                            // 这部分暂时不能自动
                            if(type.getTypeId() == new ItemStackType().getTypeId())
                                dragging = new ItemStackType(new ItemStack(BuiltInRegistries.ITEM.getHolder(BuiltInRegistries.ITEM.getKey((Item) stackKey)).get(),1,dataComponentPatch));
                            else if(type.getTypeId() == new FluidStackType().getTypeId())
                                dragging = new FluidStackType(new FluidStack(BuiltInRegistries.FLUID.getHolder(BuiltInRegistries.FLUID.getKey((Fluid) stackKey)).get(),1,dataComponentPatch));
                            else if(BeyondDimensions.MekLoaded)
                            {
                                if(type.getTypeId() == new ChemicalStackType().getTypeId())
                                    dragging = new ChemicalStackType(new mekanism.api.chemical.ChemicalStack((mekanism.api.chemical.Chemical) stackKey,1));

                            }
                        }
                    }

                    StoredStackSlot sSlot = (StoredStackSlot) slot;
                    IStackType clickItem = sSlot.getVanillaActualStack();
                    // button的数字0代表左键
                    PacketDistributor.sendToServer(new FlagSlotSetPacket(sSlot.index,clickItem,dragging));
                }

            );
        }


        // 初始化滚动按钮
        this.scroller = new BigScroller(this.leftPos+175,this.topPos+23,99,0,menu.maxLineData);
        addRenderableWidget(scroller);

        popButton = new ReverseButton(this.leftPos+72+18*4-5,this.topPos+6, button ->
        {
            popButton.toggleState();
            menu.popMode = !menu.popMode;
            PacketDistributor.sendToServer(new PopModeButtonPacket(menu.popMode));
        });
        addRenderableWidget(popButton);


        menu.unifiedStorage.getStorage().clear();
        menu.suppressRemoteUpdates();
        BeyondDimensions.LOGGER.info("客户端发送数据请求");
        PacketDistributor.sendToServer(new CallSeverStoragePacket());
    }

    @Override
    protected void containerTick() {
        //父类无操作
        //每tick自动更新搜索方案
        menu.buildIndexList(new ArrayList<>(menu.viewerUnifiedStorage.getStorage()));
        scroller.updateScrollPosition(menu.lineData,menu.maxLineData);// 读取翻页数据并应用

        if(menu.popMode)
        {
            popButton.setState(ButtonState.DISABLED);
        }
        else
        {
            popButton.setState(ButtonState.ENABLED);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        scroller.render(guiGraphics,mouseX,mouseY,partialTicks);
        popButton.render(guiGraphics,mouseX,mouseY,partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY+20, 4210752);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
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
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y)
    {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if(this.hoveredSlot instanceof StoredStackSlot sSlot)
            {
                IStackType stack = sSlot.getStack();
                stack.renderTooltip(guiGraphics,minecraft.font,x,y);
            }
            else
            {
                ItemStack itemstack = this.hoveredSlot.getItem();
                guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, x, y);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
        if (scrollY > 0)
        {
            menu.lineData--;
        } else if(scrollY < 0)
        {
            menu.lineData++;
        }
        //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
        menu.ScrollTo();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        super.mouseDragged(mouseX,mouseY,button,dragX,dragY);

        // 获取拖动物品
        if(BeyondDimensions.EMILoaded && !isDragging)
        {
            dragIngredient = dev.emi.emi.api.EmiApi.getHoveredStack((int) mouseX, (int) mouseY,true).getStack();
            if(dragIngredient != null)
                isDragging = true;
        }

        // 父类的覆写方法没有显式调用其被拖拽的子元素的拖拽方法，所以需要手动调用
        int scrollY =  scroller.customDragAction(mouseX,mouseY,button,dragX,dragY);
        if (scrollY > 0)
        {
            menu.lineData--;
        } else if(scrollY < 0)
        {
            menu.lineData++;
        }
        //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
        menu.ScrollTo();
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
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        super.mouseReleased(mouseX, mouseY, button);
        if(BeyondDimensions.EMILoaded)
        {
            if(dragIngredient != null)
                this.dragHandler.dropStack(this, dragIngredient,(int)mouseX,(int)mouseY);
            dragIngredient = null;
        }
        isDragging = false;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public Font getFont() {
        return font;
    }

}
