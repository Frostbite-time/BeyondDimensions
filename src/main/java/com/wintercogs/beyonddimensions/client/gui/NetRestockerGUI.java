package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.item.BaseMachineItem;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetRestockerMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NetRestockerGUI extends BDBaseGUI<NetRestockerMenu>
{
    private static final ResourceLocation STATS_ICON_LOCATION = ResourceLocation.tryBuild("minecraft", "textures/gui/container/stats_icons.png");
    private static final int SLOT_OFFSET_U = 0;
    private static final int SLOT_OFFSET_V = 0;
    private static final int SLOT_WIDTH = 18;
    private static final int SLOT_HEIGHT = 18;
    private static final int STATS_ICON_WIDTH = 128;
    private static final int STATS_ICON_HEIGHT = 128;


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
            BaseMachineItem.setFuzzyMode(menu.menuStack, (FuzzyMode) fuzzyModeButton.currentState);
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

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getFuzzyModeOrDefault(menu.menuStack, FuzzyMode.DISABLE));
            }
        };
        addRenderableWidget(fuzzyModeButton);

        receiveModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            receiveModeButton.toggleState();
            BaseMachineItem.setReceiveMode(menu.menuStack, (ReceiveMode) receiveModeButton.currentState);
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

                setState(BaseMachineItem.getReceiveModeOrDefault(menu.menuStack, ReceiveMode.STOP));
            }
        };
        addRenderableWidget(receiveModeButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 66, 23, 26,
                leftPos + 176 + 3, topPos + 66 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            BaseMachineItem.setControlMode(menu.menuStack, (RedStoneControlMode) controlModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(RedStoneControlMode.IGNORE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_ignore.png"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_not_working.png"));

                tooltipMap.put(RedStoneControlMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_ignore")));
                tooltipMap.put(RedStoneControlMode.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.control_mode_not_working")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE));
            }
        };
        addRenderableWidget(controlModeButton);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();

        if (fuzzyModeButton.currentState != BaseMachineItem.getFuzzyModeOrDefault(menu.menuStack, FuzzyMode.DISABLE))
            fuzzyModeButton.setState(BaseMachineItem.getFuzzyModeOrDefault(menu.menuStack, FuzzyMode.DISABLE));

        if (receiveModeButton.currentState != BaseMachineItem.getReceiveModeOrDefault(menu.menuStack, ReceiveMode.STOP))
            receiveModeButton.setState(BaseMachineItem.getReceiveModeOrDefault(menu.menuStack, ReceiveMode.STOP));

        if (controlModeButton.currentState != BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE))
            controlModeButton.setState(BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int[] drawY = new int[]{this.topPos};
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

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
            guiGraphics.blit(STATS_ICON_LOCATION,
                    this.leftPos + NetRestockerMenu.EXTRA_SLOT_START_X + i * 18 - 1,
                    this.topPos + NetRestockerMenu.EXTRA_SLOT_Y - 1,
                    SLOT_OFFSET_U,
                    SLOT_OFFSET_V,
                    SLOT_WIDTH,
                    SLOT_HEIGHT,
                    STATS_ICON_WIDTH,
                    STATS_ICON_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.restock_slots"), imageWidth - 6, this.titleLabelY + 3, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
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
