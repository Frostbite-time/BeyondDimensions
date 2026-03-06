package com.wintercogs.beyonddimensions.client.gui.widget.shared;

import com.wintercogs.beyonddimensions.client.gui.CommonTextures;

public abstract class RightTabButton extends StatusButton
{
    protected RightTabButton(int x, int y, int width, int height,
                             int iconX, int iconY, int iconWidth, int iconHeight,
                             OnPress onPress)
    {
        super(x, y, width, height, iconX, iconY, iconWidth, iconHeight, onPress);
    }

    protected RightTabButton(int x, int y, int width, int height, OnPress onPress)
    {
        this(x, y, width, height, x, y, width, height, onPress);
    }

    @Override
    public void initBackground()
    {
        setBackgroundSprites(new WidgetSprites(
                CommonTextures.RIGHT_TAB,
                CommonTextures.RIGHT_TAB
        ));
    }
}
