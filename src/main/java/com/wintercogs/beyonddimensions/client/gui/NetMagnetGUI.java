package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.LeftTabButton;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.item.BaseMachineItem;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class NetMagnetGUI extends BDBaseGUI<NetMagnetMenu>
{
    private RightTabButton filterModeButton;
    private RightTabButton controlModeButton;
    private RightTabButton hopperItemModeButton;
    private RightTabButton hopperXpModeButton;
    private RightTabButton hopperNBTModeButton;
    private RightTabButton hopperFluidModeButton;
    private LeftTabButton hopperRangeModeButton;

    public NetMagnetGUI(NetMagnetMenu menu, Inventory playerInventory, Component title)
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
            BaseMachineItem.setFilterMode(menu.menuStack, (FilterMode) filterModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FilterMode.IGNORE, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/ignore_filter.png"));
                iconMap.put(FilterMode.WHITE, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/white_filter.png"));
                iconMap.put(FilterMode.BLACK, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/black_filter.png"));

                tooltipMap.put(FilterMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_ignore")));
                tooltipMap.put(FilterMode.WHITE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_white")));
                tooltipMap.put(FilterMode.BLACK, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_black")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getFilterModeOrDefault(menu.menuStack, FilterMode.BLACK));
            }
        };
        addRenderableWidget(filterModeButton);

        controlModeButton = new RightTabButton(leftPos + 176, topPos + 36, 23, 26,
                leftPos + 176 + 3, topPos + 36 + 4, 16, 16, button -> {
            controlModeButton.toggleState();
            BaseMachineItem.setControlMode(menu.menuStack, (RedStoneControlMode) controlModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(RedStoneControlMode.IGNORE, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/control_mode_ignore.png"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/control_mode_not_working.png"));

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

        hopperItemModeButton = new RightTabButton(leftPos + 176, topPos + 66, 23, 26,
                leftPos + 176 + 3, topPos + 66 + 4, 16, 16, button -> {
            hopperItemModeButton.toggleState();
            BaseMachineItem.setHopperItemMode(menu.menuStack, (HopperItemMode) hopperItemModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperItemMode.DENY, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_item_mode_deny.png"));
                iconMap.put(HopperItemMode.ALLOW, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_item_mode_allow.png"));


                tooltipMap.put(HopperItemMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_item_mode_deny")));
                tooltipMap.put(HopperItemMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_item_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getHopperItemModeOrDefault(menu.menuStack, HopperItemMode.ALLOW));
            }
        };
        addRenderableWidget(hopperItemModeButton);

        hopperXpModeButton = new RightTabButton(leftPos + 176, topPos + 96, 23, 26,
                leftPos + 176 + 3, topPos + 96 + 4, 16, 16, button -> {
            hopperXpModeButton.toggleState();
            BaseMachineItem.setHopperXpMode(menu.menuStack, (HopperXpMode) hopperXpModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperXpMode.DENY, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_xp_mode_deny.png"));
                iconMap.put(HopperXpMode.ALLOW, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_xp_mode_allow.png"));


                tooltipMap.put(HopperXpMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_xp_mode_deny")));
                tooltipMap.put(HopperXpMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_xp_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getHopperXpModeOrDefault(menu.menuStack, HopperXpMode.ALLOW));
            }
        };
        addRenderableWidget(hopperXpModeButton);

        hopperNBTModeButton = new RightTabButton(leftPos + 176, topPos + 126, 23, 26,
                leftPos + 176 + 3, topPos + 126 + 4, 16, 16, button -> {
            hopperNBTModeButton.toggleState();
            BaseMachineItem.setHopperNBTMode(menu.menuStack, (HopperNBTMode) hopperNBTModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperNBTMode.DENY, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_nbt_mode_deny.png"));
                iconMap.put(HopperNBTMode.ALLOW, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_nbt_mode_allow.png"));


                tooltipMap.put(HopperNBTMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_nbt_mode_deny")));
                tooltipMap.put(HopperNBTMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_nbt_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getHopperNBTModeOrDefault(menu.menuStack, HopperNBTMode.DENY));
            }
        };
        addRenderableWidget(hopperNBTModeButton);

        hopperFluidModeButton = new RightTabButton(leftPos + 176, topPos + 156, 23, 26,
                leftPos + 176 + 3, topPos + 156 + 4, 16, 16, button -> {
            hopperFluidModeButton.toggleState();
            BaseMachineItem.setHopperFluidMode(menu.menuStack, (HopperFluidMode) hopperFluidModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperFluidMode.DENY, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_fluid_mode_deny.png"));
                iconMap.put(HopperFluidMode.ALLOW, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_fluid_mode_allow.png"));

                tooltipMap.put(HopperFluidMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_fluid_mode_deny")));
                tooltipMap.put(HopperFluidMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_fluid_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getHopperFluidModeOrDefault(menu.menuStack, HopperFluidMode.DENY));
            }
        };
        addRenderableWidget(hopperFluidModeButton);

        hopperRangeModeButton = new LeftTabButton(leftPos - 23, topPos + 156, 23, 26,
                leftPos - 18, topPos + 156 + 4, 16, 16, button -> {
            hopperRangeModeButton.toggleState();
            BaseMachineItem.setHopperRangeMode(menu.menuStack, (HopperRangeMode) hopperRangeModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperRangeMode.RADIUS_LOWEST, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_range_mode_lowest.png"));
                iconMap.put(HopperRangeMode.RADIUS_LOW, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_range_mode_low.png"));
                iconMap.put(HopperRangeMode.RADIUS_MID, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_range_mode_mid.png"));
                iconMap.put(HopperRangeMode.RADIUS_HIGH, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_range_mode_high.png"));
                iconMap.put(HopperRangeMode.RADIUS_HIGHEST, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_range_mode_highest.png"));
                iconMap.put(HopperRangeMode.CHUNK_MODE, ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/hopper_range_mode_chunk.png"));

                tooltipMap.put(HopperRangeMode.RADIUS_LOWEST, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.magnet_range_mode_lowest")));
                tooltipMap.put(HopperRangeMode.RADIUS_LOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.magnet_range_mode_low")));
                tooltipMap.put(HopperRangeMode.RADIUS_MID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.magnet_range_mode_mid")));
                tooltipMap.put(HopperRangeMode.RADIUS_HIGH, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.magnet_range_mode_high")));
                tooltipMap.put(HopperRangeMode.RADIUS_HIGHEST, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.magnet_range_mode_highest")));
                tooltipMap.put(HopperRangeMode.CHUNK_MODE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.magnet_range_mode_chunk")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(BaseMachineItem.getHopperRangeModeOrDefault(menu.menuStack, HopperRangeMode.RADIUS_MID));
            }
        };
        addRenderableWidget(hopperRangeModeButton);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (filterModeButton.currentState != BaseMachineItem.getFilterModeOrDefault(menu.menuStack, FilterMode.BLACK))
            filterModeButton.setState(BaseMachineItem.getFilterModeOrDefault(menu.menuStack, FilterMode.BLACK));

        if (controlModeButton.currentState != BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE))
            controlModeButton.setState(BaseMachineItem.getControlModeOrDefault(menu.menuStack, RedStoneControlMode.IGNORE));

        if (hopperItemModeButton.currentState != BaseMachineItem.getHopperItemModeOrDefault(menu.menuStack, HopperItemMode.ALLOW))
            hopperItemModeButton.setState(BaseMachineItem.getHopperItemModeOrDefault(menu.menuStack, HopperItemMode.ALLOW));

        if (hopperXpModeButton.currentState != BaseMachineItem.getHopperXpModeOrDefault(menu.menuStack, HopperXpMode.DENY))
            hopperXpModeButton.setState(BaseMachineItem.getHopperXpModeOrDefault(menu.menuStack, HopperXpMode.DENY));

        if (hopperNBTModeButton.currentState != BaseMachineItem.getHopperNBTModeOrDefault(menu.menuStack, HopperNBTMode.DENY))
            hopperNBTModeButton.setState(BaseMachineItem.getHopperNBTModeOrDefault(menu.menuStack, HopperNBTMode.DENY));

        if (hopperFluidModeButton.currentState != BaseMachineItem.getHopperFluidModeOrDefault(menu.menuStack, HopperFluidMode.DENY))
            hopperFluidModeButton.setState(BaseMachineItem.getHopperFluidModeOrDefault(menu.menuStack, HopperFluidMode.DENY));

        if (hopperRangeModeButton.currentState != BaseMachineItem.getHopperRangeModeOrDefault(menu.menuStack, HopperRangeMode.RADIUS_MID))
            hopperRangeModeButton.setState(BaseMachineItem.getHopperRangeModeOrDefault(menu.menuStack, HopperRangeMode.RADIUS_MID));

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

