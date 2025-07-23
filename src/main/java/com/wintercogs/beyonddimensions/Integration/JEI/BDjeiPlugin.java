package com.wintercogs.beyonddimensions.Integration.JEI;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.GUI.BDBaseGUI;
import com.wintercogs.beyonddimensions.Integration.JEI.ContainerHandler.JeiContainerHandler;
import com.wintercogs.beyonddimensions.Integration.JEI.RecipeTransfer.CraftMenuRecipeTransferHandler;
import com.wintercogs.beyonddimensions.Integration.JEI.RecipeTransfer.CraftTerminalRecipeTransferHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class BDjeiPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return ResourceLocation.tryBuild(BeyondDimensions.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration)
    {
        registration.addRecipeTransferHandler(new CraftMenuRecipeTransferHandler(), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new CraftTerminalRecipeTransferHandler(), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addGhostIngredientHandler(BDBaseGUI.class, new NetInterfaceGhostHandler());
        registration.addGenericGuiContainerHandler(BDBaseGUI.class, new JeiContainerHandler());
    }
}
