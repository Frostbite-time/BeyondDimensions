package com.wintercogs.beyonddimensions.client.gui.widget.shared;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class IconButton extends Button implements GuiElementAccess
{
    protected Identifier icon;

    protected final int iconX;
    protected final int iconY;
    protected final int iconWidth;
    protected final int iconHeight;

    protected WidgetSprites backgroundSprites = new WidgetSprites(
            BeyondDimensions.makeId("widget/slot_button"),
            BeyondDimensions.makeId("widget/slot_button_disabled"),
            BeyondDimensions.makeId("widget/slot_button_hovered")
    );

    // 从左到右的含义分别为
    // x起始、y起始、宽、高、组件、按钮名称（父类为按钮上的字）、按下按钮后的行为、叙述（使用默认叙述即可）
    public IconButton(int x, int y, int width, int height, Identifier icon,
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

    public IconButton(int x, int y, int width, int height, Identifier icon,
                      OnPress onPress)
    {
        this(x, y, width, height, icon, x, y, width, height, onPress);
    }

    @Override
    protected void renderContents(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, backgroundSprites.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        drawIcon(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onRelease(@NotNull MouseButtonEvent event)
    {
        super.onRelease(event);
        setFocused(false);
    }

    protected void drawIcon(GuiGraphics st, int mouseX, int mouseY, float pt)
    {
        st.blitSprite(RenderPipelines.GUI_TEXTURED, getIcon(), iconX, iconY, iconWidth, iconHeight);
    }

    // 用于覆写背景
    public void initBackground()
    {

    }

    public void setBackgroundSprites(WidgetSprites backgroundSprites)
    {
        this.backgroundSprites = backgroundSprites;
    }

    public Identifier getIcon()
    {
        return icon;
    }

    public void setIcon(Identifier icon)
    {
        this.icon = icon;
    }


    @Override
    public Rect2i getElementArea() //Rect2i是小对象，每帧重新分配应当相当安全
    {
        return new Rect2i(getX(), getY(), getWidth(), getHeight());
    }
}
