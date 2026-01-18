package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.wintercogs.beyonddimensions.GUI.CommonTextures;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;

public abstract class LeftTabButton extends StatusButton
{
    protected LeftTabButton(int x, int y, int width, int height,
                            int iconX, int iconY, int iconWidth, int iconHeight,
                            Button.OnPress onPress)
    {
        super(x, y, width, height, iconX, iconY, iconWidth, iconHeight, onPress);
    }

    protected LeftTabButton(int x, int y, int width, int height, Button.OnPress onPress)
    {
        this(x, y, width, height, x, y, width, height, onPress);
    }

    @Override
    public void initBackground()
    {
        setBackgroundSprites(new WidgetSprites(
                CommonTextures.LEFT_TAB,
                CommonTextures.LEFT_TAB
        ));
    }
}
