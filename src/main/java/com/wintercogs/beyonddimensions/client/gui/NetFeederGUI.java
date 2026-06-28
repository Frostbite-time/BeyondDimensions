package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.FeederMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetFeederMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class NetFeederGUI extends BDBaseGUI<NetFeederMenu>
{
    private RightTabButton controlModeButton;
    private RightTabButton feederModeButton;

    public NetFeederGUI(NetFeederMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init()
    {
        super.init();

        this.imageWidth = 176;
        this.imageHeight = rebuildImageHeight();
        rebuildLabelHeight();
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;

        feederModeButton = new RightTabButton(leftPos + 176, topPos + 6, 23, 26,
                leftPos + 176 + 3, topPos + 6 + 4, 16, 16, button -> {
            feederModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.FEEDER_MODE, (FeederMode) feederModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FeederMode.HUNGER_TO_EAT, BeyondDimensions.makeId("widget/feeder_mode_hunger_to_eat"));
                iconMap.put(FeederMode.NORMAL, BeyondDimensions.makeId("widget/feeder_mode_normal"));
                iconMap.put(FeederMode.SATURATION_KEEP, BeyondDimensions.makeId("widget/feeder_mode_saturation_keep"));
                iconMap.put(FeederMode.CRAZY, BeyondDimensions.makeId("widget/feeder_mode_crazy"));

                tooltipMap.put(FeederMode.HUNGER_TO_EAT, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_hunger_to_eat")));
                tooltipMap.put(FeederMode.NORMAL, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_normal")));
                tooltipMap.put(FeederMode.SATURATION_KEEP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_saturation_keep")));
                tooltipMap.put(FeederMode.CRAZY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_crazy")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.FEEDER_MODE));
            }
        };
        addRenderableWidget(feederModeButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.CONTROL_MODE, (RedStoneControlMode) controlModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(RedStoneControlMode.IGNORE, BeyondDimensions.makeId("widget/control_mode_ignore"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, BeyondDimensions.makeId("widget/control_mode_not_working"));

                tooltipMap.put(RedStoneControlMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_ignore")));
                tooltipMap.put(RedStoneControlMode.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_not_working")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.CONTROL_MODE));
            }
        };
        addRenderableWidget(controlModeButton);


    }

    @Override
    protected void containerTick()
    {
        super.containerTick();

        if (controlModeButton.currentState != menu.menuStack.get(BDDataComponents.CONTROL_MODE))
            controlModeButton.setState(menu.menuStack.get(BDDataComponents.CONTROL_MODE));

        if (feederModeButton.currentState != menu.menuStack.get(BDDataComponents.FEEDER_MODE))
            feederModeButton.setState(menu.menuStack.get(BDDataComponents.FEEDER_MODE));

    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a)
    {
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
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int xm, int ym)
    {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.filter_slots"), imageWidth - 6, this.titleLabelY + 3, -12566464, false);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + 4;
    }
}
