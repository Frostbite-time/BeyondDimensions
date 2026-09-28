package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CraftTerminalRecipeTransferHandler implements IRecipeTransferHandler<DimensionsCraftMenuTerminal, RecipeHolder<CraftingRecipe>>
{


    public CraftTerminalRecipeTransferHandler()
    {
    }

    @Override
    public Class<? extends DimensionsCraftMenuTerminal> getContainerClass()
    {
        return DimensionsCraftMenuTerminal.class;
    }

    @Override
    public Optional<MenuType<DimensionsCraftMenuTerminal>> getMenuType()
    {
        return Optional.of(DimensionsCraftMenuTerminal.Dimensions_Craft_Menu_Terminal.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType()
    {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(DimensionsCraftMenuTerminal container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(container, recipeSlots, maxTransfer, doTransfer, false);
    }




}
