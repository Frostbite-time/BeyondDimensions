package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchToggleButton extends StatusButton
{
    public SearchToggleButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, ButtonName.SearchButton, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap = new HashMap<>();
        iconMap.put(ButtonState.DISABLED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/search_disable"));
        iconMap.put(ButtonState.ENABLED,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/search_enable"));

        this.states = new ArrayList<>();
        for(ButtonState state : iconMap.keySet())
        {
            this.states.add(state);
        }
        this.currentState = Config.uiSearchButton;
    }
}
