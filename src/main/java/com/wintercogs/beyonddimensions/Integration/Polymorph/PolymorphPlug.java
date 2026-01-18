package com.wintercogs.beyonddimensions.Integration.Polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.wintercogs.beyonddimensions.GUI.DimensionsCraftGUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PolymorphPlug
{
    public static void register()
    {

        PolymorphApi.client().registerWidget(screen -> {
            if (screen instanceof DimensionsCraftGUI<?> gui)
                return new RecipeWidget(gui, gui.getMenu().getSlot(gui.getMenu().resultSlotIndex));

            return null;
        });
    }
}
