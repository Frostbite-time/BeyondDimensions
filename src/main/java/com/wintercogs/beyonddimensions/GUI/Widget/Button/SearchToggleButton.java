package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SearchToggleButton extends StatusButton
{
    public SearchToggleButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, ButtonName.SearchButton, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap.put(ButtonState.DISABLED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/search_disable"));
        iconMap.put(ButtonState.ENABLED,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/search_enable"));

        tooltipMap.put(ButtonState.DISABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.search_disable")));
        tooltipMap.put(ButtonState.ENABLED, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.search_enable")));

        for(ButtonState state : iconMap.keySet())
        {
            this.states.add(state);
        }
        setState(Config.uiSearchButton);
    }
}
