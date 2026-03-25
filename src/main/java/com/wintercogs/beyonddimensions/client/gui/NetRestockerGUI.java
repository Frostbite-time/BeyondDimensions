package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetRestockerMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class NetRestockerGUI extends BDBaseGUI<NetRestockerMenu>
{
    private static final Identifier VANILLA_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private RightTabButton fuzzyModeButton;
    private RightTabButton receiveModeButton;
    private RightTabButton controlModeButton;

    public NetRestockerGUI(NetRestockerMenu menu, Inventory playerInventory, Component title)
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

        fuzzyModeButton = new RightTabButton(leftPos + 176, topPos + 6, 23, 26,
                leftPos + 176 + 3, topPos + 6 + 4, 16, 16, button -> {
            fuzzyModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.FUZZY_MODE, (FuzzyMode) fuzzyModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FuzzyMode.DISABLE, BeyondDimensions.makeId("widget/hopper_nbt_mode_allow"));
                iconMap.put(FuzzyMode.ENABLE, BeyondDimensions.makeId("widget/hopper_nbt_mode_deny"));

                tooltipMap.put(FuzzyMode.DISABLE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.fuzzy_mode_disable")));
                tooltipMap.put(FuzzyMode.ENABLE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.fuzzy_mode_enable")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.FUZZY_MODE));
            }
        };
        addRenderableWidget(fuzzyModeButton);

        receiveModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            receiveModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.RECEIVE_MODE, (ReceiveMode) receiveModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(ReceiveMode.STOP, BeyondDimensions.makeId("widget/net_disable"));
                iconMap.put(ReceiveMode.OPEN, BeyondDimensions.makeId("widget/net_absorb"));

                tooltipMap.put(ReceiveMode.STOP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.receive_mode_stop")));
                tooltipMap.put(ReceiveMode.OPEN, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.receive_mode_open")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.RECEIVE_MODE));
            }
        };
        addRenderableWidget(receiveModeButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 66, 23, 26,
                leftPos + 176 + 3, topPos + 66 + 4, 16, 16, button -> {
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

        if (fuzzyModeButton.currentState != menu.menuStack.get(BDDataComponents.FUZZY_MODE))
            fuzzyModeButton.setState(menu.menuStack.get(BDDataComponents.FUZZY_MODE));

        if (receiveModeButton.currentState != menu.menuStack.get(BDDataComponents.RECEIVE_MODE))
            receiveModeButton.setState(menu.menuStack.get(BDDataComponents.RECEIVE_MODE));

        if (controlModeButton.currentState != menu.menuStack.get(BDDataComponents.CONTROL_MODE))
            controlModeButton.setState(menu.menuStack.get(BDDataComponents.CONTROL_MODE));
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor guiGraphics)
    {
        int[] drawY = new int[]{this.topPos};

        CommonTexturesRender.renderTopBaseCommon(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics, this.leftPos, drawY);

        for (int i = 0; i < 5; i++)
        {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_SLOT_SPRITE,
                    this.leftPos + NetRestockerMenu.EXTRA_SLOT_START_X + i * 18 - 1,
                    this.topPos + NetRestockerMenu.EXTRA_SLOT_Y - 1,
                    18,
                    18);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int xm, int ym)
    {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.restock_slots"), imageWidth - 6, this.titleLabelY + 3, -12566464, false);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 2 + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + CommonTextures.COMMON_CONNECTION_HEIGHT + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 2 + CommonTextures.FILTER_SLOTS_HEIGHT * 4 + 4;
    }
}
