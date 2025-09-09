package com.wintercogs.beyonddimensions.GUI.Widget.Button;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
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
        iconMap.put(ButtonState.SORT_QUANTITY,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_quantity"));
        iconMap.put(ButtonState.SORT_NAME,ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_name"));
        iconMap.put(ButtonState.SORT_MODID, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_modid"));
        iconMap.put(ButtonState.SORT_INSERTED_TIME, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_inserted_time"));
        iconMap.put(ButtonState.SORT_MODIFIED_TIME, ResourceLocation.tryBuild(BeyondDimensions.MODID,"widget/sort_modified_time"));

        tooltipMap.put(ButtonState.SORT_QUANTITY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_quantity")));
        tooltipMap.put(ButtonState.SORT_NAME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_name")));
        tooltipMap.put(ButtonState.SORT_MODID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_modid")));
        tooltipMap.put(ButtonState.SORT_INSERTED_TIME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_inserted_time")));
        tooltipMap.put(ButtonState.SORT_MODIFIED_TIME, Tooltip.create(Component.translatable(("tooltip.button.beyonddimensions.sort_modified_time"))));

        for(Enum<?> state : iconMap.keySet())
        {
            this.states.add(state);
        }
        setState(Config.uiSortButton);
    }
}
