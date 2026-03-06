package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.machine.AutoSortMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetFurnaceMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NetFurnaceGUI extends BDBaseGUI<NetFurnaceMenu>
{
    private RightTabButton popModeButton; // 弹出模式
    private RightTabButton controlModeButton;
    private RightTabButton receiveModeButton;
    private RightTabButton sortModeButton;

    public NetFurnaceGUI(NetFurnaceMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init()
    {
        super.init();

        this.imageWidth = 230;
        this.imageHeight = 210;
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;

        popModeButton = new RightTabButton(this.leftPos + imageWidth, this.topPos + 6, 23, 26,
                this.leftPos + imageWidth + 3, this.topPos + 6 + 4, 16, 16, button ->
        {
            popModeButton.toggleState();
            menu.be.popMode = (PopMode) popModeButton.currentState;
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


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.popMode);
            }
        };
        addRenderableWidget(popModeButton);

        receiveModeButton = new RightTabButton(leftPos + imageWidth, topPos + 36, 23, 26,
                leftPos + imageWidth + 3, topPos + 36 + 4, 16, 16, button -> {
            receiveModeButton.toggleState();
            menu.be.receiveMode = (ReceiveMode) receiveModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(ReceiveMode.STOP, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/net_disable.png"));
                iconMap.put(ReceiveMode.OPEN, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/net_absorb.png"));

                tooltipMap.put(ReceiveMode.STOP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.receive_mode_stop")));
                tooltipMap.put(ReceiveMode.OPEN, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.receive_mode_open")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.receiveMode);
            }
        };
        addRenderableWidget(receiveModeButton);

        controlModeButton = new RightTabButton(leftPos + imageWidth, topPos + 66, 23, 26,
                leftPos + imageWidth + 3, topPos + 66 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            menu.be.controlMode = (RedStoneControlMode) controlModeButton.currentState;
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


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.controlMode);
            }
        };
        addRenderableWidget(controlModeButton);

        sortModeButton = new RightTabButton(leftPos + imageWidth, topPos + 96, 23, 26,
                leftPos + imageWidth + 3, topPos + 96 + 4, 16, 16, button -> {
            sortModeButton.toggleState();
            menu.be.sortMode = (AutoSortMode) sortModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(AutoSortMode.OPEN, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_mode_open.png"));
                iconMap.put(AutoSortMode.STOP, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_mode_stop.png"));

                tooltipMap.put(AutoSortMode.OPEN, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_mode_open")));
                tooltipMap.put(AutoSortMode.STOP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_mode_stop")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.sortMode);
            }
        };
        addRenderableWidget(sortModeButton);

    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (controlModeButton.currentState != menu.be.controlMode)
            controlModeButton.setState(menu.be.controlMode);

        if (popModeButton.currentState != menu.be.popMode)
            popModeButton.setState(menu.be.popMode);

        if (receiveModeButton.currentState != menu.be.receiveMode)
            receiveModeButton.setState(menu.be.receiveMode);

        if (sortModeButton.currentState != menu.be.sortMode)
            sortModeButton.setState(menu.be.sortMode);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        CommonTexturesRender.renderNetFurnaceBackground(guiGraphics, leftPos, new int[]{topPos});

        // 绘制熔炼进度 getCapacity为同时处理的任务数
        for (int i = 0; i < menu.be.getCapacity(); i++)
        {
            float progress = (float) menu.be.getCookTime().get(i) / (float) menu.be.getCookTimeTotal().get(i);
            CommonTexturesRender.renderWorkDoneV_AsProgress(guiGraphics, leftPos + 32 + i * 19, new int[]{topPos + 61}, CommonTextures.WORK_DONE_V_WIDTH, CommonTextures.WORK_DONE_V_HEIGHT, progress);
        }
        // 绘制燃料进度
        for (int i = 0; i < menu.be.getCapacity(); i++)
        {
            float progress = (float) menu.be.getLitTime().get(i) / menu.be.getLitDuration().get(i);
            CommonTexturesRender.renderFurnaceWorkV_AsProgress(guiGraphics, leftPos + 31 + i * 19, new int[]{topPos + 109}, CommonTextures.FURNACE_WORK_V_WIDTH, CommonTextures.FURNACE_WORK_V_HEIGHT, progress);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.label.beyonddimensions.input_filter_slots"), 6, 27, 4210752, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.fuel_filter_slots"), 224, 27, 4210752, false);
    }

    protected int rebuildImageHeight()
    {
        return 210;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelX = 6;
        this.inventoryLabelY = 190;
    }
}

