package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class IconButton extends Button
{
    protected ButtonName name;
    protected ResourceLocation icon;

    protected static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/slot_button"),
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/slot_button_disabled"),
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "textures/gui/sprites/widget/slot_button_hovered")
    );

    // 从左到右的含义分别为
    // x起始、y起始、宽、高、组件、按钮名称（父类为按钮上的字）、按下按钮后的行为、叙述（使用默认叙述即可）
    protected IconButton(int x, int y, int width, int height,ResourceLocation icon ,ButtonName name, OnPress onPress)
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
            ResourceLocation texture = SPRITES.get(this.active,this.isHoveredOrFocused());

            st.blit(texture, x, y, 0,0,this.width,this.height ,16 ,16 );
            st.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        setFocused(false);
    }

    protected void drawIcon(GuiGraphics st, int mouseX, int mouseY, float pt) {
        st.blit(getIcon(), this.getX(), this.getY(), 0,0,this.getWidth(), this.getHeight(),16,16);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public void setIcon(ResourceLocation icon)
    {
        this.icon = icon;
    }

    public ButtonName getName()
    {
        return this.name;
    }


}
