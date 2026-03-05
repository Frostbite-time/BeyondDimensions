package com.wintercogs.beyonddimensions.client.gui;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.LeftTabButton;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.machine.*;
import com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu;
import com.wintercogs.beyonddimensions.util.GuiRenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
            menu.menuStack.set(BDDataComponents.FILTER_MODE, (FilterMode) filterModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(FilterMode.IGNORE, Identifier.tryBuild(BeyondDimensions.MODID, "widget/ignore_filter"));
                iconMap.put(FilterMode.WHITE, Identifier.tryBuild(BeyondDimensions.MODID, "widget/white_filter"));
                iconMap.put(FilterMode.BLACK, Identifier.tryBuild(BeyondDimensions.MODID, "widget/black_filter"));

                tooltipMap.put(FilterMode.IGNORE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_ignore")));
                tooltipMap.put(FilterMode.WHITE, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_white")));
                tooltipMap.put(FilterMode.BLACK, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.filter_mode_black")));

                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.FILTER_MODE));
            }
        };
        addRenderableWidget(filterModeButton);

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
                iconMap.put(RedStoneControlMode.IGNORE, Identifier.tryBuild(BeyondDimensions.MODID, "widget/control_mode_ignore"));
                iconMap.put(RedStoneControlMode.NOT_WORKING, Identifier.tryBuild(BeyondDimensions.MODID, "widget/control_mode_not_working"));

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

        hopperItemModeButton = new RightTabButton(leftPos + 176, topPos + 66, 23, 26,
                leftPos + 176 + 3, topPos + 66 + 4, 16, 16, button -> {
            hopperItemModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.HOPPER_ITEM_MODE, (HopperItemMode) hopperItemModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperItemMode.DENY, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_item_mode_deny"));
                iconMap.put(HopperItemMode.ALLOW, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_item_mode_allow"));


                tooltipMap.put(HopperItemMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_item_mode_deny")));
                tooltipMap.put(HopperItemMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_item_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.HOPPER_ITEM_MODE));
            }
        };
        addRenderableWidget(hopperItemModeButton);

        hopperXpModeButton = new RightTabButton(leftPos + 176, topPos + 96, 23, 26,
                leftPos + 176 + 3, topPos + 96 + 4, 16, 16, button -> {
            hopperXpModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.HOPPER_XP_MODE, (HopperXpMode) hopperXpModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperXpMode.DENY, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_xp_mode_deny"));
                iconMap.put(HopperXpMode.ALLOW, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_xp_mode_allow"));


                tooltipMap.put(HopperXpMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_xp_mode_deny")));
                tooltipMap.put(HopperXpMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_xp_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.HOPPER_XP_MODE));
            }
        };
        addRenderableWidget(hopperXpModeButton);

        hopperNBTModeButton = new RightTabButton(leftPos + 176, topPos + 126, 23, 26,
                leftPos + 176 + 3, topPos + 126 + 4, 16, 16, button -> {
            hopperNBTModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.HOPPER_NBT_MODE, (HopperNBTMode) hopperNBTModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperNBTMode.DENY, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_nbt_mode_deny"));
                iconMap.put(HopperNBTMode.ALLOW, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_nbt_mode_allow"));


                tooltipMap.put(HopperNBTMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_nbt_mode_deny")));
                tooltipMap.put(HopperNBTMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_nbt_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.HOPPER_NBT_MODE));
            }
        };
        addRenderableWidget(hopperNBTModeButton);

        hopperFluidModeButton = new RightTabButton(leftPos + 176, topPos + 156, 23, 26,
                leftPos + 176 + 3, topPos + 156 + 4, 16, 16, button -> {
            hopperFluidModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.HOPPER_FLUID_MODE, (HopperFluidMode) hopperFluidModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperFluidMode.DENY, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_fluid_mode_deny"));
                iconMap.put(HopperFluidMode.ALLOW, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_fluid_mode_allow"));

                tooltipMap.put(HopperFluidMode.DENY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_fluid_mode_deny")));
                tooltipMap.put(HopperFluidMode.ALLOW, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.hopper_fluid_mode_allow")));


                for (Enum<?> state : iconMap.keySet())
                {
                    this.states.add(state);
                }

                setState(menu.menuStack.get(BDDataComponents.HOPPER_FLUID_MODE));
            }
        };
        addRenderableWidget(hopperFluidModeButton);

        hopperRangeModeButton = new LeftTabButton(leftPos - 23, topPos + 156, 23, 26,
                leftPos - 18, topPos + 156 + 4, 16, 16, button -> {
            hopperRangeModeButton.toggleState();
            menu.menuStack.set(BDDataComponents.HOPPER_RANGE_MODE, (HopperRangeMode) hopperRangeModeButton.currentState);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(HopperRangeMode.RADIUS_LOWEST, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_range_mode_lowest"));
                iconMap.put(HopperRangeMode.RADIUS_LOW, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_range_mode_low"));
                iconMap.put(HopperRangeMode.RADIUS_MID, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_range_mode_mid"));
                iconMap.put(HopperRangeMode.RADIUS_HIGH, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_range_mode_high"));
                iconMap.put(HopperRangeMode.RADIUS_HIGHEST, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_range_mode_highest"));
                iconMap.put(HopperRangeMode.CHUNK_MODE, Identifier.tryBuild(BeyondDimensions.MODID, "widget/hopper_range_mode_chunk"));

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

                setState(menu.menuStack.get(BDDataComponents.HOPPER_RANGE_MODE));
            }
        };
        addRenderableWidget(hopperRangeModeButton);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (filterModeButton.currentState != menu.menuStack.get(BDDataComponents.FILTER_MODE))
            filterModeButton.setState(menu.menuStack.get(BDDataComponents.FILTER_MODE));

        if (controlModeButton.currentState != menu.menuStack.get(BDDataComponents.CONTROL_MODE))
            controlModeButton.setState(menu.menuStack.get(BDDataComponents.CONTROL_MODE));

        if (hopperItemModeButton.currentState != menu.menuStack.get(BDDataComponents.HOPPER_ITEM_MODE))
            hopperItemModeButton.setState(menu.menuStack.get(BDDataComponents.HOPPER_ITEM_MODE));

        if (hopperXpModeButton.currentState != menu.menuStack.get(BDDataComponents.HOPPER_XP_MODE))
            hopperXpModeButton.setState(menu.menuStack.get(BDDataComponents.HOPPER_XP_MODE));

        if (hopperNBTModeButton.currentState != menu.menuStack.get(BDDataComponents.HOPPER_NBT_MODE))
            hopperNBTModeButton.setState(menu.menuStack.get(BDDataComponents.HOPPER_NBT_MODE));

        if (hopperFluidModeButton.currentState != menu.menuStack.get(BDDataComponents.HOPPER_FLUID_MODE))
            hopperFluidModeButton.setState(menu.menuStack.get(BDDataComponents.HOPPER_FLUID_MODE));

        if (hopperRangeModeButton.currentState != menu.menuStack.get(BDDataComponents.HOPPER_RANGE_MODE))
            hopperRangeModeButton.setState(menu.menuStack.get(BDDataComponents.HOPPER_RANGE_MODE));

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        GuiRenderHelper.drawRightAnchoredText(guiGraphics, this.font, Component.translatable("menu.label.beyonddimensions.filter_slots"), imageWidth - 6, this.titleLabelY + 3, -12566464, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
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
