package com.wintercogs.beyonddimensions.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.RightTabButton;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings;
import com.wintercogs.beyonddimensions.common.menu.XpExchangeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

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
        this.targetLevelField.setTextColor(0xFFFFFFFF);
        this.targetLevelField.setTooltip(Tooltip.create(Component.translatable("tooltip.editbox.beyonddimensions.xp_exchange_target_level")));
        this.targetLevelField.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        this.targetLevelField.setValue(Integer.toString(XpExchangeSettings.getTargetLevel(menu.menuStack)));
        this.targetLevelField.setResponder(this::onTargetLevelChanged);
        addRenderableWidget(this.targetLevelField);

        keepModeButton = new RightTabButton(leftPos + 176, topPos + 6, 23, 26,
                leftPos + 179, topPos + 10, 16, 16, button -> {
            KeepModeState nextState = keepModeButton.currentState == KeepModeState.WORKING ? KeepModeState.NOT_WORKING : KeepModeState.WORKING;
            keepModeButton.setState(nextState);
            menu.menuStack.set(BDDataComponents.XP_NET_KEEP_MODE, nextState == KeepModeState.WORKING);
            menu.writeAndSendQuickData();
        })
        {
            @Override
            protected void initButton()
            {
                iconMap.put(KeepModeState.WORKING, BeyondDimensions.makeId("widget/control_mode_ignore"));
                iconMap.put(KeepModeState.NOT_WORKING, BeyondDimensions.makeId("widget/control_mode_not_working"));

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (targetLevelField != null && targetLevelField.isMouseOver(mouseX, mouseY) && scrollY != 0)
        {
            var window = Minecraft.getInstance().getWindow();
            boolean controlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
            boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            int step = controlDown ? 100 : (shiftDown ? 10 : 1);
            int direction = scrollY > 0 ? 1 : -1;
            int currentValue = targetLevelField.getValue().isEmpty() ? 0 : Integer.parseInt(targetLevelField.getValue());
            int nextValue = XpExchangeSettings.sanitizeTargetLevel(currentValue + direction * step);

            syncingField = true;
            targetLevelField.setValue(Integer.toString(nextValue));
            syncingField = false;

            XpExchangeSettings.setTargetLevel(menu.menuStack, nextValue);
            menu.writeAndSendQuickData();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private KeepModeState resolveKeepModeState()
    {
        return menu.menuStack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE, false) ? KeepModeState.WORKING : KeepModeState.NOT_WORKING;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int[] drawY = new int[]{this.topPos};
        CommonTexturesRender.renderTopBaseCommon(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderCommonConnection(guiGraphics, this.leftPos, drawY);
        CommonTexturesRender.renderPlayerInv(guiGraphics, this.leftPos, drawY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        guiGraphics.text(this.font, Component.translatable("menu.label.beyonddimensions.xp_exchange.target_level"), 8, 27, -12566464, false);
        guiGraphics.text(this.font, Component.translatable("menu.label.beyonddimensions.xp_exchange.max_level", XpExchangeSettings.MAX_TARGET_LEVEL), 8, 41, -12566464, false);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
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
