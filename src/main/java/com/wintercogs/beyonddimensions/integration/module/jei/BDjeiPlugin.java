package com.wintercogs.beyonddimensions.integration.module.jei;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.integration.module.jei.ContainerHandler.JeiContainerHandler;
import com.wintercogs.beyonddimensions.integration.module.jei.RecipeTransfer.CraftMenuRecipeTransferHandler;
import com.wintercogs.beyonddimensions.integration.module.jei.RecipeTransfer.CraftTerminalRecipeTransferHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

@JeiPlugin
public class BDjeiPlugin implements IModPlugin
{
    public static IJeiRuntime jeiRuntime;

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

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime)
    {
        BDjeiPlugin.jeiRuntime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable()
    {
        BDjeiPlugin.jeiRuntime = null;
    }

    public static Optional<IJeiRuntime> runtime()
    {
        return Optional.ofNullable(BDjeiPlugin.jeiRuntime);
    }
}
