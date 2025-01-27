package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;

public class SortMethodButton extends StatusButton
{
    public SortMethodButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, ButtonName.SortMethodButton, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap = new HashMap<>();
        iconMap.put(ButtonState.SORT_DEFAULT, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_default"));
        iconMap.put(ButtonState.SORT_QUANTITY,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_quantity"));
        iconMap.put(ButtonState.SORT_NAME,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_name"));

        this.states = new ArrayList<>();
        for(ButtonState state : iconMap.keySet())
        {
            this.states.add(state);
        }
        this.currentState = ButtonState.SORT_DEFAULT;
    }
}
