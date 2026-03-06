package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.LeftTabButton;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.NetHopperMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NetHopperGUI extends BDBaseGUI<NetHopperMenu>
{
    private RightTabButton filterModeButton;
    private RightTabButton controlModeButton;
    private RightTabButton hopperItemModeButton;
    private RightTabButton hopperXpModeButton;
    private RightTabButton hopperNBTModeButton;
    private RightTabButton hopperFluidModeButton;
    private LeftTabButton hopperRangeModeButton;

    public NetHopperGUI(NetHopperMenu menu, Inventory playerInventory, Component title)
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

        filterModeButton = new RightTabButton(leftPos + 176, topPos + 6, 23, 26,
                leftPos + 176 + 3, topPos + 6 + 4, 16, 16, button -> {
            filterModeButton.toggleState();
            menu.be.filterMode = (FilterMode) filterModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FilterMode.IGNORE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/ignore_filter.png"));
                iconMap.put(FilterMode.WHITE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/white_filter.png"));
                iconMap.put(FilterMode.BLACK, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/black_filter.png"));

                tooltipMap.put(FilterMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_ignore")));
                tooltipMap.put(FilterMode.WHITE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_white")));
                tooltipMap.put(FilterMode.BLACK, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_black")));

                for (Enum<?> state : iconMap.keySet())
                {
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

        hopperItemModeButton = new RightTabButton(leftPos + 176, topPos + 66, 23, 26,
                leftPos + 176 + 3, topPos + 66 + 4, 16, 16, button -> {
            hopperItemModeButton.toggleState();
            menu.be.hopperItemMode = (HopperItemMode) hopperItemModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperItemMode.DENY, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_item_mode_deny.png"));
                iconMap.put(HopperItemMode.ALLOW, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_item_mode_allow.png"));


                tooltipMap.put(HopperItemMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_item_mode_deny")));
                tooltipMap.put(HopperItemMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_item_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.hopperItemMode);
            }
        };
        addRenderableWidget(hopperItemModeButton);

        hopperXpModeButton = new RightTabButton(leftPos + 176, topPos + 96, 23, 26,
                leftPos + 176 + 3, topPos + 96 + 4, 16, 16, button -> {
            hopperXpModeButton.toggleState();
            menu.be.hopperXpMode = (HopperXpMode) hopperXpModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperXpMode.DENY, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_xp_mode_deny.png"));
                iconMap.put(HopperXpMode.ALLOW, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_xp_mode_allow.png"));


                tooltipMap.put(HopperXpMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_xp_mode_deny")));
                tooltipMap.put(HopperXpMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_xp_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.hopperXpMode);
            }
        };
        addRenderableWidget(hopperXpModeButton);

        hopperNBTModeButton = new RightTabButton(leftPos + 176, topPos + 126, 23, 26,
                leftPos + 176 + 3, topPos + 126 + 4, 16, 16, button -> {
            hopperNBTModeButton.toggleState();
            menu.be.hopperNBTMode = (HopperNBTMode) hopperNBTModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperNBTMode.DENY, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_nbt_mode_deny.png"));
                iconMap.put(HopperNBTMode.ALLOW, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_nbt_mode_allow.png"));


                tooltipMap.put(HopperNBTMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_nbt_mode_deny")));
                tooltipMap.put(HopperNBTMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_nbt_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.hopperNBTMode);
            }
        };
        addRenderableWidget(hopperNBTModeButton);

        hopperFluidModeButton = new RightTabButton(leftPos + 176, topPos + 156, 23, 26,
                leftPos + 176 + 3, topPos + 156 + 4, 16, 16, button -> {
            hopperFluidModeButton.toggleState();
            menu.be.hopperFluidMode = (HopperFluidMode) hopperFluidModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperFluidMode.DENY, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_fluid_mode_deny.png"));
                iconMap.put(HopperFluidMode.ALLOW, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_fluid_mode_allow.png"));

                tooltipMap.put(HopperFluidMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_fluid_mode_deny")));
                tooltipMap.put(HopperFluidMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_fluid_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.hopperFluidMode);
            }
        };
        addRenderableWidget(hopperFluidModeButton);

        hopperRangeModeButton = new LeftTabButton(leftPos - 23, topPos + 156, 23, 26,
                leftPos - 18, topPos + 156 + 4, 16, 16, button -> {
            hopperRangeModeButton.toggleState();
            menu.be.hopperRangeMode = (HopperRangeMode) hopperRangeModeButton.currentState;
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperRangeMode.RADIUS_LOWEST, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_range_mode_lowest.png"));
                iconMap.put(HopperRangeMode.RADIUS_LOW, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_range_mode_low.png"));
                iconMap.put(HopperRangeMode.RADIUS_MID, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_range_mode_mid.png"));
                iconMap.put(HopperRangeMode.RADIUS_HIGH, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_range_mode_high.png"));
                iconMap.put(HopperRangeMode.RADIUS_HIGHEST, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_range_mode_highest.png"));
                iconMap.put(HopperRangeMode.CHUNK_MODE, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/hopper_range_mode_chunk.png"));

                tooltipMap.put(HopperRangeMode.RADIUS_LOWEST, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_range_mode_lowest")));
                tooltipMap.put(HopperRangeMode.RADIUS_LOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_range_mode_low")));
                tooltipMap.put(HopperRangeMode.RADIUS_MID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_range_mode_mid")));
                tooltipMap.put(HopperRangeMode.RADIUS_HIGH, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_range_mode_high")));
                tooltipMap.put(HopperRangeMode.RADIUS_HIGHEST, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_range_mode_highest")));
                tooltipMap.put(HopperRangeMode.CHUNK_MODE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_range_mode_chunk")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.be.hopperRangeMode);
            }
        };
        addRenderableWidget(hopperRangeModeButton);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (filterModeButton.currentState != menu.be.filterMode)
            filterModeButton.setState(menu.be.filterMode);

        if (controlModeButton.currentState != menu.be.controlMode)
            controlModeButton.setState(menu.be.controlMode);

        if (hopperItemModeButton.currentState != menu.be.hopperItemMode)
            hopperItemModeButton.setState(menu.be.hopperItemMode);

        if (hopperXpModeButton.currentState != menu.be.hopperXpMode)
            hopperXpModeButton.setState(menu.be.hopperXpMode);

        if (hopperNBTModeButton.currentState != menu.be.hopperNBTMode)
            hopperNBTModeButton.setState(menu.be.hopperNBTMode);

        if (hopperFluidModeButton.currentState != menu.be.hopperFluidMode)
            hopperFluidModeButton.setState(menu.be.hopperFluidMode);

        if (hopperRangeModeButton.currentState != menu.be.hopperRangeMode)
            hopperRangeModeButton.setState(menu.be.hopperRangeMode);

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
