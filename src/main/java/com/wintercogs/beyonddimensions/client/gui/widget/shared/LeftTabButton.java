package com.wintercogs.beyonddimensions.client.gui.widget.shared;

import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import net.minecraft.client.gui.components.Button;

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
