package com.wintercogs.beyonddimensions.Integration.EMI;

import com.wintercogs.beyonddimensions.Integration.EMI.Recipe.NetRecipeHandler;
import com.wintercogs.beyonddimensions.Integration.EMI.SlotHandler.SlotDragHandler;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenuTerminal;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class BDEMIPlugin implements EmiPlugin
{
    @Override
    public void register(EmiRegistry registry)
    {
        registry.addRecipeHandler(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), new NetRecipeHandler());
        registry.addRecipeHandler(DimensionsCraftMenuTerminal.Dimensions_Craft_Menu_Terminal.get(), new NetRecipeHandler());
        registry.addGenericDragDropHandler(new SlotDragHandler());
    }
}
