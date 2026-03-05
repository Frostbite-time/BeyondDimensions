package com.wintercogs.beyonddimensions.integration.jei;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.BDBaseGUI;
import com.wintercogs.beyonddimensions.integration.jei.handler.JeiContainerHandler;
import com.wintercogs.beyonddimensions.integration.jei.transfer.CraftMenuRecipeTransferHandler;
import com.wintercogs.beyonddimensions.integration.jei.transfer.CraftTerminalRecipeTransferHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@JeiPlugin
public class BDJEIPlugin implements IModPlugin
{
    public static IJeiRuntime jeiRuntime;

    @Override
    public @NotNull Identifier getPluginUid()
    {
        return BeyondDimensions.makeId("jei_plugin");
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
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime)
    {
        BDJEIPlugin.jeiRuntime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable()
    {
        BDJEIPlugin.jeiRuntime = null;
    }

    public static Optional<IJeiRuntime> runtime()
    {
        return Optional.ofNullable(BDJEIPlugin.jeiRuntime);
    }
}
