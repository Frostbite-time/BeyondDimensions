package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.wintercogs.beyonddimensions.GUI.CommonTextures;

public abstract class RightTabButton extends StatusButton
{
    protected RightTabButton(int x, int y, int width, int height, OnPress onPress)
    {
        super(x, y, width, height, onPress);
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
