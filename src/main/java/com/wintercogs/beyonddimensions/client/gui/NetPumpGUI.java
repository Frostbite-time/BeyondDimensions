package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.machine.FilterMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class NetPumpGUI extends BDBaseGUI<NetPumpMenu> {
    private RightTabButton filterModeButton;
    private RightTabButton controlModeButton;

    public NetPumpGUI(NetPumpMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.imageWidth = 176;
        this.imageHeight = rebuildImageHeight();
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;

        filterModeButton = new RightTabButton(leftPos + 176, topPos + 6, 23, 26,
                leftPos + 176 + 3, topPos + 6 + 4, 16, 16, button -> {
            filterModeButton.toggleState();
            menu.be.filterMode = (FilterMode) filterModeButton.currentState;
            menu.writeAndSendQuickData();
        }) {
            @Override
            protected void initButton() {
                iconMap.put(FilterMode.IGNORE, BeyondDimensions.makeId("widget/ignore_filter"));
                iconMap.put(FilterMode.WHITE, BeyondDimensions.makeId("widget/white_filter"));
                iconMap.put(FilterMode.BLACK, BeyondDimensions.makeId("widget/black_filter"));

                tooltipMap.put(FilterMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_ignore")));
                tooltipMap.put(FilterMode.WHITE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_white")));
                tooltipMap.put(FilterMode.BLACK, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_black")));

                for (Enum<?> state : iconMap.keySet()) {
                    this.states.add(state);
                }

                setState(menu.be.filterMode);
            }
        };
        addRenderableWidget(filterModeButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            menu.be.controlMode = (RedStoneControlMode) controlModeButton.currentState;
            menu.writeAndSendQuickData();
        }) {
            @Override
            protected void initButton() {
                iconMap.put(RedStoneControlMode.IGNORE, BeyondDimensions.makeId("widget/control_mode_ignore"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, BeyondDimensions.makeId("widget/control_mode_not_working"));
                iconMap.put(RedStoneControlMode.POWERED, BeyondDimensions.makeId("widget/control_mode_powered"));
                iconMap.put(RedStoneControlMode.UNPOWERED, BeyondDimensions.makeId("widget/control_mode_unpowered"));


                tooltipMap.put(RedStoneControlMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_ignore")));
                tooltipMap.put(RedStoneControlMode.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_not_working")));
                tooltipMap.put(RedStoneControlMode.POWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_powered")));
                tooltipMap.put(RedStoneControlMode.UNPOWERED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_unpowered")));


                for (Enum<?> state : iconMap.keySet()) {
                    this.states.add(state);
                }

                setState(menu.be.controlMode);
            }
        };
        addRenderableWidget(controlModeButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (filterModeButton.currentState != menu.be.filterMode)
            filterModeButton.setState(menu.be.filterMode);

        if (controlModeButton.currentState != menu.be.controlMode)
            controlModeButton.setState(menu.be.controlMode);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);
        int[] drawY = new int[]{this.topPos}; // 用于动态控制绘制
        CommonTexturesRender.renderTopBaseCommon(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics, this.leftPos, drawY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int xm, int ym) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.filter_slots"), imageWidth - 6, this.titleLabelY + 3, -12566464, false);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected int rebuildImageHeight() {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight() {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + 4;
    }
}
