package com.wintercogs.beyonddimensions.client.gui.widget.shared;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IconButton extends Button implements GuiElementAccess
{
    protected ResourceLocation icon;

    protected final int iconX;
    protected final int iconY;
    protected final int iconWidth;
    protected final int iconHeight;

    protected WidgetSprites backgroundSprites = new WidgetSprites(
            ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/slot_button.png"),
            ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/slot_button_disabled.png"),
            ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/slot_button_hovered.png")
    );

    // 从左到右的含义分别为
    // x起始、y起始、宽、高、组件、按钮名称（父类为按钮上的字）、按下按钮后的行为、叙述（使用默认叙述即可）
    public IconButton(int x, int y, int width, int height, ResourceLocation icon,
                      int iconX, int iconY, int iconWidth, int iconHeight,
                      OnPress onPress)
    {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.icon = icon;
        this.iconX = iconX;
        this.iconY = iconY;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        initBackground();
    }

    public IconButton(int x, int y, int width, int height, ResourceLocation icon,
                      OnPress onPress)
    {
        this(x, y, width, height, icon, x, y, width, height, onPress);
    }

    @Override
    public void renderWidget(GuiGraphics st, int mouseX, int mouseY, float pt)
    {

        if (this.visible)
        {
            int x = getX();
            int y = getY();
            st.setColor(1.0f, 1.0f, 1.0f, this.alpha);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            this.isHovered = mouseX >= x && mouseY >= y && mouseX < x + this.width && mouseY < y + this.height;
            ResourceLocation texture = backgroundSprites.get(this.active, this.isHoveredOrFocused());

            st.blit(texture, x, y, 0, 0, this.width, this.height, this.width, this.height);
            drawIcon(st, mouseX, mouseY, pt);
            st.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        setFocused(false);
    }

    protected void drawIcon(GuiGraphics st, int mouseX, int mouseY, float pt)
    {
        st.blit(getIcon(), iconX, iconY, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);
    }

    // 用于覆写背景
    public void initBackground()
    {

    }

    public void setBackgroundSprites(WidgetSprites backgroundSprites)
    {
        this.backgroundSprites = backgroundSprites;
    }

    public ResourceLocation getIcon()
    {
        return icon;
    }

    public void setIcon(ResourceLocation icon)
    {
        this.icon = icon;
    }

    @Override
    public Rect2i getElementArea() //Rect2i是小对象，每帧重新分配应当相当安全
    {
        return new Rect2i(getX(), getY(), getWidth(), getHeight());
    }
}
