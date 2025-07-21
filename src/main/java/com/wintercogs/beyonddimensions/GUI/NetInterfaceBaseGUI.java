package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import com.wintercogs.beyonddimensions.Integration.EMI.SlotHandler.SlotDragHandler;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


public class NetInterfaceBaseGUI extends BDBaseGUI<NetInterfaceBaseMenu>
{

    public StatusButton popButton; // 使用倒序按钮来临时替代弹出模式

    private SlotDragHandler dragHandler; // 仅在emi加载时可用

    private dev.emi.emi.api.stack.EmiIngredient dragIngredient; // 仅在emi加载时可用
    private boolean isDragging = false;


    public NetInterfaceBaseGUI(NetInterfaceBaseMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
    }



    @Override
    protected void init() {
        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.imageWidth = 176;
        this.imageHeight = rebuildImageHeight();
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth)/2;
        this.topPos = (this.height - imageHeight)/2;


        popButton = new StatusButton(this.leftPos+72+18*4-5,this.topPos+6,16,16, button ->
        {
            popButton.toggleState();
            menu.be.popMode = !menu.be.popMode;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(ButtonState.ENABLED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_asc.png"));
                iconMap.put(ButtonState.DISABLED,ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_desc.png"));

                tooltipMap.put(ButtonState.ENABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_on")));
                tooltipMap.put(ButtonState.DISABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_off")));


                for(Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                if(menu.be.popMode)
                {
                    setState(ButtonState.ENABLED);
                }
                else
                {
                    setState(ButtonState.DISABLED);
                }
            }
        };
        addRenderableWidget(popButton);

    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_SLOTS_HEIGHT*3 + CommonTextures.FILTER_SLOTS_HEIGHT*3+CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_SLOTS_HEIGHT*3 + CommonTextures.FILTER_SLOTS_HEIGHT*3+4;
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();


        if(menu.be.popMode)
        {
            popButton.setState(ButtonState.ENABLED);
        }
        else
        {
            popButton.setState(ButtonState.DISABLED);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int[] drawY = new int[]{this.topPos}; // 用于动态控制绘制
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CommonTexturesRender.renderTopBaseCommon(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics,this.leftPos,drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics,this.leftPos,drawY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        popButton.render(guiGraphics,mouseX,mouseY,partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752,false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752,false);
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

}
