package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;


public class NetInterfaceBaseGUI extends BDBaseGUI<NetInterfaceBaseMenu>
{

    public RightTabButton popButton; // 弹出模式
    public RightTabButton controlModeButton; // 红石控制模式按钮
    public RightTabButton fuzzyModelButton; //模糊匹配模式

    public NetInterfaceBaseGUI(NetInterfaceBaseMenu container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        // 去除空白的真实部分，用于计算图片显示的最佳位置

    }


    @Override
    protected void init()
    {
        super.init();

        // 如果以后图片大小有变，显示中心所期望的大小仍然是x:176,y:235用于计算
        this.imageWidth = 176;
        this.imageHeight = rebuildImageHeight();
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;


        popButton = new RightTabButton(this.leftPos + 176, this.topPos + 6, 23, 26,
                this.leftPos + 176 + 3, this.topPos + 6 + 4, 16, 16, button ->
        {
            if (!menu.getAccess().canConfigurePopMode())
            {
                return;
            }
            popButton.toggleState();
            menu.getAccess().setPopMode((PopMode) popButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(PopMode.OPEN, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/popmode_up.png"));
                iconMap.put(PopMode.STOP, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/popmode_down.png"));

                tooltipMap.put(PopMode.OPEN, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_on")));
                tooltipMap.put(PopMode.STOP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_off")));


                this.states.addAll(iconMap.keySet());

                setState(menu.getAccess().getPopMode());
            }
        };
        if (!menu.getAccess().canConfigurePopMode())
        {
            popButton.active = false;
            popButton.setTooltip(Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.popmode_mounted_unavailable")));
        }
        addRenderableWidget(popButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            menu.getAccess().setControlMode((RedStoneControlMode) controlModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(RedStoneControlMode.IGNORE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_ignore.png"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_not_working.png"));
                iconMap.put(RedStoneControlMode.POWERED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_powered.png"));
                iconMap.put(RedStoneControlMode.UNPOWERED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_unpowered.png"));

                tooltipMap.put(RedStoneControlMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_ignore")));
                tooltipMap.put(RedStoneControlMode.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_not_working")));
                tooltipMap.put(RedStoneControlMode.POWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_powered")));
                tooltipMap.put(RedStoneControlMode.UNPOWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_unpowered")));

                this.states.addAll(iconMap.keySet());

                setState(menu.getAccess().getControlMode());
            }
        };
        addRenderableWidget(controlModeButton);

        fuzzyModelButton = new RightTabButton(leftPos + 176, topPos + 66, 23, 26,
                leftPos + 176 + 3, topPos + 66 + 4, 16, 16, button -> {
            fuzzyModelButton.toggleState();
            menu.getAccess().setFuzzyMode((FuzzyMode) fuzzyModelButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FuzzyMode.DISABLE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_nbt_mode_allow.png"));
                iconMap.put(FuzzyMode.ENABLE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_nbt_mode_deny.png"));

                tooltipMap.put(FuzzyMode.DISABLE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.fuzzy_mode_disable")));
                tooltipMap.put(FuzzyMode.ENABLE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.fuzzy_mode_enable")));

                this.states.addAll(iconMap.keySet());

                setState(menu.getAccess().getFuzzyMode());
            }
        };
        addRenderableWidget(fuzzyModelButton);
    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_SLOTS_HEIGHT * 3 + CommonTextures.FILTER_SLOTS_HEIGHT * 3 + CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_SLOTS_HEIGHT * 3 + CommonTextures.FILTER_SLOTS_HEIGHT * 3 + 4;
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        //父类无操作
        //每tick自动更新搜索方案

        if (popButton.currentState != menu.getAccess().getPopMode())
            popButton.setState(menu.getAccess().getPopMode());
        if (controlModeButton.currentState != menu.getAccess().getControlMode())
            controlModeButton.setState(menu.getAccess().getControlMode());
        if (fuzzyModelButton.currentState != menu.getAccess().getFuzzyMode())
            fuzzyModelButton.setState(menu.getAccess().getFuzzyMode());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int[] drawY = new int[]{this.topPos}; // 用于动态控制绘制
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CommonTexturesRender.renderTopBaseCommon(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics, this.leftPos, drawY);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        popButton.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.tag_and_stored_slots"), imageWidth - 6, this.titleLabelY + 3, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

}
