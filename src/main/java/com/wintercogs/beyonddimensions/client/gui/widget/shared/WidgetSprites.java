package com.wintercogs.beyonddimensions.client.gui.widget.shared;

import net.minecraft.resources.ResourceLocation;

public record WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused,
                            ResourceLocation disabledFocused)
{
    public WidgetSprites(ResourceLocation noFocused, ResourceLocation focused)
    {
        this(noFocused, noFocused, focused, focused);
    }

    public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused)
    {
        this(enabled, disabled, enabledFocused, disabled);
    }

    public WidgetSprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused, ResourceLocation disabledFocused)
    {
        this.enabled = enabled;
        this.disabled = disabled;
        this.enabledFocused = enabledFocused;
        this.disabledFocused = disabledFocused;
    }

    public ResourceLocation get(boolean enabled, boolean focused)
    {
        if (enabled)
        {
            return focused ? this.enabledFocused : this.enabled;
        }
        else
        {
            return focused ? this.disabledFocused : this.disabled;
        }
    }

    public ResourceLocation enabled()
    {
        return this.enabled;
    }

    public ResourceLocation disabled()
    {
        return this.disabled;
    }

    public ResourceLocation enabledFocused()
    {
        return this.enabledFocused;
    }

    public ResourceLocation disabledFocused()
    {
        return this.disabledFocused;
    }

}
