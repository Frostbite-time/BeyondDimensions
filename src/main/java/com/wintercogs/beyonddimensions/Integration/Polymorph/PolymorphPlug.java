package com.wintercogs.beyonddimensions.Integration.Polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.wintercogs.beyonddimensions.GUI.DimensionsCraftGUI;

public class PolymorphPlug
{
    public static void register()
    {

        PolymorphApi.client().registerWidget(screen ->{
            if(screen instanceof DimensionsCraftGUI gui)
                return new RecipeWidget(gui, gui.getMenu().getSlot(gui.getMenu().resultSlotIndex));

            return null;
        });
    }
}
