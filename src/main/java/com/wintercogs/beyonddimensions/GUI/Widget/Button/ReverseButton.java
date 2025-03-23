package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;

public class ReverseButton extends StatusButton
{

    public ReverseButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, ButtonName.ReverseButton, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap = new HashMap<>();
        iconMap.put(ButtonState.DISABLED, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_asc"));
        iconMap.put(ButtonState.ENABLED,ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_desc"));

        this.states = new ArrayList<>();
        for(ButtonState state : iconMap.keySet())
        {
            this.states.add(state);
        }
        this.currentState = ButtonState.DISABLED;
    }
}
