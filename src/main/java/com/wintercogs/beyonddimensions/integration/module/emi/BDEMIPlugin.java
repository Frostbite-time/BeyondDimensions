package com.wintercogs.beyonddimensions.integration.module.emi;

import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.integration.module.emi.exclusion.BDExclusionZones;
import com.wintercogs.beyonddimensions.integration.module.emi.recipe.NetRecipeHandler;
import com.wintercogs.beyonddimensions.integration.module.emi.slothandler.SlotDragHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class BDEMIPlugin implements EmiPlugin
{
    @Override
    public void register(EmiRegistry registry)
    {
        registry.addRecipeHandler(BDMenus.Dimensions_Craft_Menu.get(), new NetRecipeHandler());
        registry.addRecipeHandler(BDMenus.Dimensions_Craft_Menu_Terminal.get(), new NetRecipeHandler());
        registry.addGenericDragDropHandler(new SlotDragHandler());
        registry.addGenericExclusionArea(new BDExclusionZones());
    }
}
