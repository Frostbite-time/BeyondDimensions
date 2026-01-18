package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.GUI.SharedWidget.StatusButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SortMethodButton extends StatusButton
{
    public SortMethodButton(int x, int y, OnPress onPress)
    {
        super(x, y, 16, 16, onPress);
    }

    @Override
    protected void initButton()
    {
        iconMap.put(ButtonState.SORT_DEFAULT, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_default.png"));
        iconMap.put(ButtonState.SORT_QUANTITY,ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_quantity.png"));
        iconMap.put(ButtonState.SORT_NAME,ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_name.png"));
        iconMap.put(ButtonState.SORT_MODID, ResourceLocation.tryBuild(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_modid.png"));

        tooltipMap.put(ButtonState.SORT_DEFAULT, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_default")));
        tooltipMap.put(ButtonState.SORT_QUANTITY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_quantity")));
        tooltipMap.put(ButtonState.SORT_NAME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_name")));
        tooltipMap.put(ButtonState.SORT_MODID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_modid")));

        for(Enum<?> state : iconMap.keySet())
        {
            this.states.add(state);
        }
        setState(CommonConfigRuntime.uiSortButton);
    }
}
