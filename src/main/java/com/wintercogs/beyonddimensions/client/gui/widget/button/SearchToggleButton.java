package com.wintercogs.beyonddimensions.client.gui.widget.button;

import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.StatusButton;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SearchToggleButton extends StatusButton
{
    public SearchToggleButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap.put(ButtonState.DISABLED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/search_disable.png"));
        iconMap.put(ButtonState.ENABLED, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/search_enable.png"));

        tooltipMap.put(ButtonState.DISABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.search_disable")));
        tooltipMap.put(ButtonState.ENABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.search_enable")));

        for (Enum<?> state : iconMap.keySet())
        {
            this.states.add(state);
        }
        setState(CommonConfigRuntime.uiSearchButton);
    }
}
