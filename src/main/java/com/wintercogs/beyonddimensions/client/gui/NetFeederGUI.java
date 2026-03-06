package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.item.BaseMachineItem;
import com.wintercogs.beyonddimensions.common.machine.FeederMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.menu.NetFeederMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
            BaseMachineItem.setFeederMode(menu.menuStack, (FeederMode) feederModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FeederMode.HUNGER_TO_EAT, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/feeder_mode_hunger_to_eat.png"));
                iconMap.put(FeederMode.NORMAL, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/feeder_mode_normal.png"));
                iconMap.put(FeederMode.SATURATION_KEEP, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/feeder_mode_saturation_keep.png"));
                iconMap.put(FeederMode.CRAZY, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/feeder_mode_crazy.png"));

                tooltipMap.put(FeederMode.HUNGER_TO_EAT, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_hunger_to_eat")));
                tooltipMap.put(FeederMode.NORMAL, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_normal")));
                tooltipMap.put(FeederMode.SATURATION_KEEP, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_saturation_keep")));
                tooltipMap.put(FeederMode.CRAZY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.feeder_mode_crazy")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }
                setState(BaseMachineItem.getFeederModeOrDefault(menu.menuStack, FeederMode.NORMAL));
            }
        };
        addRenderableWidget(feederModeButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 2, topPos + 36 + 5, 16, 16, button -> {
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

        if (controlModeButton.currentState != BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE))
            controlModeButton.setState(BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE));

        if (feederModeButton.currentState != BaseMachineItem.getFeederModeOrDefault(menu.menuStack, FeederMode.NORMAL))
            feederModeButton.setState(BaseMachineItem.getFeederModeOrDefault(menu.menuStack, FeederMode.NORMAL));

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int[] drawY = new int[]{this.topPos}; // 用于动态控制绘制
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CommonTexturesRender.renderTopBaseCommon(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderFilterSlots(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics, this.leftPos, drawY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.filter_slots"), imageWidth - 6, this.titleLabelY + 3, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
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
