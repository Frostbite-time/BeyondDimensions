package com.wintercogs.beyonddimensions.GUI.Widget.Scroller;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.ScrollBar;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BigScroller extends ScrollBar
{
    public static final ResourceLocation sprite = ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/big_scroller");
    public BigScroller(int x, int y, int maxScrollLength, int currentPosition, int maxPosition)
    {
        super(x, y, 15, 20, sprite, maxScrollLength, currentPosition, maxPosition, Component.empty());
    }
}
