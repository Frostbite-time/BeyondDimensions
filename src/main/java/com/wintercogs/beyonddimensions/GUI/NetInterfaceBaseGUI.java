package com.wintercogs.beyonddimensions.GUI;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.RightTabButton;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.Render.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


public class NetInterfaceBaseGUI extends BDBaseGUI<NetInterfaceBaseMenu>
{

    public RightTabButton popButton; // 弹出模式
    public RightTabButton controlModeButton; // 红石控制模式按钮


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


        popButton = new RightTabButton(this.leftPos+176,this.topPos+6,23,26,
                this.leftPos+176 +3,this.topPos+6+4,16,16,button ->
        {
            popButton.toggleState();
            menu.be.popMode = (PopMode) popButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(PopMode.OPEN, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/popmode_up.png"));
                iconMap.put(PopMode.STOP,ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/popmode_down.png"));

                tooltipMap.put(PopMode.OPEN, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_on")));
                tooltipMap.put(PopMode.STOP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_off")));


                for(Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.popMode);
            }
        };
        addRenderableWidget(popButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos +36, 23,26 ,
                leftPos + 176 +3 , topPos +36 +4, 16,16,button -> {
            controlModeButton.toggleState();
            menu.be.controlMode = (RedStoneControlMode) controlModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(RedStoneControlMode.IGNORE, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/control_mode_ignore.png"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/control_mode_not_working.png"));
                iconMap.put(RedStoneControlMode.POWERED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/control_mode_powered.png"));
                iconMap.put(RedStoneControlMode.UNPOWERED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/control_mode_unpowered.png"));

                tooltipMap.put(RedStoneControlMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_ignore")));
                tooltipMap.put(RedStoneControlMode.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_not_working")));
                tooltipMap.put(RedStoneControlMode.POWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_powered")));
                tooltipMap.put(RedStoneControlMode.UNPOWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_unpowered")));

                for(Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.controlMode);
            }
        };
        addRenderableWidget(controlModeButton);

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


        if(popButton.currentState != menu.be.popMode)
            popButton.setState(menu.be.popMode);
        if(controlModeButton.currentState != menu.be.controlMode)
            controlModeButton.setState(menu.be.controlMode);
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
        GuiRenderHelper.drawRightAnchoredText(guiGraphics,this.font, Component.translatable("menu.label.beyonddimensions.tag_and_stored_slots"), imageWidth-6, this.titleLabelY+3, 4210752,false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752,false);
    }

}
