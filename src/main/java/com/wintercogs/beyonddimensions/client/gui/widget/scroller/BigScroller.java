package com.wintercogs.beyonddimensions.client.gui.widget.scroller;

import com.wintercogs.beyonddimensions.client.gui.widget.shared.ScrollBar;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

public class BigScroller extends ScrollBar
{
    public static final ResourceLocation sprite = ResourceLocation.tryBuild("minecraft", "container/creative_inventory/scroller");

    public BigScroller(int x, int y, int maxScrollLength, int currentPosition, int maxPosition, @Nullable IntConsumer onScroll)
    {
        super(x, y, 12, 15, sprite, maxScrollLength, currentPosition, maxPosition, onScroll, Component.empty());
    }
}
