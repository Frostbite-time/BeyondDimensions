package com.wintercogs.beyonddimensions.Integration.EMI;

import com.wintercogs.beyonddimensions.Integration.EMI.Recipe.NetRecipeHandler;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class BDEMIPlugin implements EmiPlugin
{
    @Override
    public void register(EmiRegistry registry)
    {
        registry.addRecipeHandler(UIRegister.Dimensions_Craft_Menu.get(), new NetRecipeHandler());
        registry.addRecipeHandler(UIRegister.Dimensions_Craft_Menu_Terminal.get(), new NetRecipeHandler());
    }
}
