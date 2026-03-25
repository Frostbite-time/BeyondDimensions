package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.item.XpExchangeItem;
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings;
import com.wintercogs.beyonddimensions.common.menu.XpExchangeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class XpExchangeGUI extends BDBaseGUI<XpExchangeMenu>
{
    private RightTabButton keepModeButton;
    private EditBox targetLevelField;
    private boolean syncingField;

    public XpExchangeGUI(XpExchangeMenu menu, Inventory playerInventory, Component title)
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

        this.targetLevelField = new EditBox(getFont(), this.leftPos + 50, this.topPos + 24, 82, this.getFont().lineHeight + 6,
                Component.translatable("menu.label.beyonddimensions.xp_exchange.target_level"));
        this.targetLevelField.setMaxLength(6);
        this.targetLevelField.setBordered(true);
        this.targetLevelField.setVisible(true);
        this.targetLevelField.setTextColor(16777215);
        this.targetLevelField.setTooltip(Tooltip.create(Component.translatable("tooltip.editbox.beyonddimensions.xp_exchange_target_level")));
        this.targetLevelField.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.targetLevelField.setValue(Integer.toString(XpExchangeSettings.getTargetLevel(menu.menuStack)));
        this.targetLevelField.setResponder(this::onTargetLevelChanged);
        addRenderableWidget(this.targetLevelField);

        keepModeButton = new RightTabButton(leftPos + 176, topPos + 6, 23, 26,
                leftPos + 176 + 3, topPos + 6 + 4, 16, 16, button -> {
            KeepModeState nextState = keepModeButton.currentState == KeepModeState.WORKING ? KeepModeState.NOT_WORKING : KeepModeState.WORKING;
            keepModeButton.setState(nextState);
            XpExchangeItem.setXpNetKeepMode(menu.menuStack, nextState == KeepModeState.WORKING);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(KeepModeState.WORKING, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_ignore.png"));
                iconMap.put(KeepModeState.NOT_WORKING, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/control_mode_not_working.png"));

                tooltipMap.put(KeepModeState.WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.xp_exchange.keep_mode_working")));
                tooltipMap.put(KeepModeState.NOT_WORKING, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.xp_exchange.keep_mode_not_working")));

                this.states.add(KeepModeState.WORKING);
                this.states.add(KeepModeState.NOT_WORKING);
                setState(resolveKeepModeState());
            }
        };
        addRenderableWidget(keepModeButton);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();

        KeepModeState keepModeState = resolveKeepModeState();
        if (keepModeButton.currentState != keepModeState)
            keepModeButton.setState(keepModeState);

        String currentTargetLevel = Integer.toString(XpExchangeSettings.getTargetLevel(menu.menuStack));
        if (!targetLevelField.isFocused() && !targetLevelField.getValue().equals(currentTargetLevel))
        {
            syncingField = true;
            targetLevelField.setValue(currentTargetLevel);
            syncingField = false;
        }
    }

    private void onTargetLevelChanged(String text)
    {
        if (syncingField)
            return;

        int targetLevel = text.isEmpty() ? 0 : Integer.parseInt(text);
        int sanitizedTargetLevel = XpExchangeSettings.sanitizeTargetLevel(targetLevel);
        XpExchangeSettings.setTargetLevel(menu.menuStack, sanitizedTargetLevel);
        if (!text.isEmpty())
        {
            String sanitizedText = Integer.toString(sanitizedTargetLevel);
            if (!sanitizedText.equals(text))
            {
                syncingField = true;
                targetLevelField.setValue(sanitizedText);
                syncingField = false;
            }
        }
        menu.writeAndSendQuickData();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (targetLevelField != null && targetLevelField.isMouseOver(mouseX, mouseY) && delta != 0)
        {
            int step = hasControlDown() ? 100 : (hasShiftDown() ? 10 : 1);
            int direction = delta > 0 ? 1 : -1;
            int currentValue = targetLevelField.getValue().isEmpty() ? 0 : Integer.parseInt(targetLevelField.getValue());
            int nextValue = XpExchangeSettings.sanitizeTargetLevel(currentValue + direction * step);

            syncingField = true;
            targetLevelField.setValue(Integer.toString(nextValue));
            syncingField = false;

            XpExchangeSettings.setTargetLevel(menu.menuStack, nextValue);
            menu.writeAndSendQuickData();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private KeepModeState resolveKeepModeState()
    {
        return XpExchangeItem.getOrDefaultXpNetKeepMode(menu.menuStack, false) ? KeepModeState.WORKING : KeepModeState.NOT_WORKING;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int[] drawY = new int[]{this.topPos};
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        CommonTexturesRender.renderTopBaseCommon(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics, this.leftPos, drawY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.label.beyonddimensions.xp_exchange.target_level"), 8, 27, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.label.beyonddimensions.xp_exchange.max_level", XpExchangeSettings.MAX_TARGET_LEVEL), 8, 41, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    protected int rebuildImageHeight()
    {
        return CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 5 + CommonTextures.PLAYER_INV_HEIGHT;
    }

    protected void rebuildLabelHeight()
    {
        this.titleLabelY = 8;
        this.inventoryLabelY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 4 + 4;
    }

    private enum KeepModeState
    {
        WORKING,
        NOT_WORKING
    }
}
