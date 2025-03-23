package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import net.minecraft.resources.ResourceLocation;

public class WidgetSprites
{
    ResourceLocation defaultIcon;
    ResourceLocation disabledIcon;
    ResourceLocation hoveredIcon;

    public WidgetSprites(ResourceLocation defaultIcon, ResourceLocation disabledIcon, ResourceLocation hoveredIcon)
    {
        this.defaultIcon = defaultIcon;
        this.disabledIcon = disabledIcon;
        this.hoveredIcon = hoveredIcon;
    }

    public ResourceLocation get(boolean active, boolean hovered)
    {
        if(active)
        {
            if(hovered)
                return hoveredIcon;
            else
                return defaultIcon;
        }
        else
        {
            return disabledIcon;
        }
    }

}
