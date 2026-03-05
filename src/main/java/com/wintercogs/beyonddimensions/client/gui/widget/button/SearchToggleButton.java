package com.wintercogs.beyonddimensions.client.gui.widget.button;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.StatusButton;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class SearchToggleButton extends StatusButton
{
    public SearchToggleButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap.put(ButtonState.DISABLED, BeyondDimensions.makeId("widget/search_disable"));
        iconMap.put(ButtonState.ENABLED, BeyondDimensions.makeId("widget/search_enable"));

        tooltipMap.put(ButtonState.DISABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.search_disable")));
        tooltipMap.put(ButtonState.ENABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.search_enable")));

        for (Enum<?> state : iconMap.keySet())
        {
            this.states.add(state);
        }
        setState(CommonConfigRuntime.uiSearchButton);
    }
}
