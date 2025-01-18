package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class IconButton extends Button
{
    protected Component name;
    protected ResourceLocation icon;

    protected static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "widget/slot_button"),
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "widget/slot_button_disabled"),
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "widget/slot_button_hovered")
    );

    // 从左到右的含义分别为
    // x起始、y起始、宽、高、组件、按钮名称（父类为按钮上的字）、按下按钮后的行为、叙述（使用默认叙述即可）
    protected IconButton(int x, int y, int width, int height,ResourceLocation icon ,Component name, OnPress onPress)
    {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.icon = icon;
        this.name = name;
    }

    @Override
    public void renderWidget(GuiGraphics st, int mouseX, int mouseY, float pt) {
        if (this.visible) {
            int x = getX();
            int y = getY();
            st.setColor(1.0f, 1.0f, 1.0f, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            this.isHovered = mouseX >= x && mouseY >= y && mouseX < x + this.width && mouseY < y + this.height;
            st.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            drawIcon(st, mouseX, mouseY, pt);
            st.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    protected void drawIcon(GuiGraphics st, int mouseX, int mouseY, float pt) {
        st.blitSprite(getIcon(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public void setIcon(ResourceLocation icon)
    {
        this.icon = icon;
    }

    public String getName()
    {
        if(!Objects.equals(this.name, Component.empty()))
        {
            return name.getString();
        }
        return null;
    }


}
