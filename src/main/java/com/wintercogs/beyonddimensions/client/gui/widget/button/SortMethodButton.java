package com.wintercogs.beyonddimensions.client.gui.widget.button;

import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.widget.shared.StatusButton;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
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
        iconMap.put(ButtonState.SORT_CREATIVE_TAB, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_creative_tab.png"));
        iconMap.put(ButtonState.SORT_MAX_STACK, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_max_stack.png"));
        iconMap.put(ButtonState.SORT_QUANTITY, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_quantity.png"));
        iconMap.put(ButtonState.SORT_NAME, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_name.png"));
        iconMap.put(ButtonState.SORT_MODID, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_modid.png"));
        iconMap.put(ButtonState.SORT_INSERTED_TIME, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_inserted_time.png"));
        iconMap.put(ButtonState.SORT_MODIFIED_TIME, ResourceLocation.tryBuild(BDConstants.MODID, "textures/gui/sprites/widget/sort_modified_time.png"));

        tooltipMap.put(ButtonState.SORT_CREATIVE_TAB, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_creative_tab")));
        tooltipMap.put(ButtonState.SORT_MAX_STACK, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_max_stack")));
        tooltipMap.put(ButtonState.SORT_QUANTITY, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_quantity")));
        tooltipMap.put(ButtonState.SORT_NAME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_name")));
        tooltipMap.put(ButtonState.SORT_MODID, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_modid")));
        tooltipMap.put(ButtonState.SORT_INSERTED_TIME, Tooltip.create(Component.translatable("tooltip.button.beyonddimensions.sort_inserted_time")));
        tooltipMap.put(ButtonState.SORT_MODIFIED_TIME, Tooltip.create(Component.translatable(("tooltip.button.beyonddimensions.sort_modified_time"))));

        for (Enum<?> state : iconMap.keySet())
        {
            this.states.add(state);
        }
        setState(CommonConfigRuntime.uiSortButton);
    }
}
