package com.wintercogs.beyonddimensions.integration.polymorph;

import com.illusivesoulworks.polymorph.api.client.PolymorphWidgets;
import com.wintercogs.beyonddimensions.GUI.DimensionsCraftGUI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// 用于实现对多态合成的支持
@OnlyIn(Dist.CLIENT)
public class PolymorphPlug
{
    public static void register()
    {
        PolymorphWidgets.getInstance().registerWidget(screen -> {
            if (screen instanceof DimensionsCraftGUI<?> gui)
                return new RecipeWidget(gui, gui.getMenu().getSlot(gui.getMenu().resultSlotIndex));

            return null;
        });
    }
}
