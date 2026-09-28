package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
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

public class CraftMenuRecipeTransferHandler implements IRecipeTransferHandler<DimensionsCraftMenu, RecipeHolder<CraftingRecipe>>
{


    public CraftMenuRecipeTransferHandler()
    {
    }

    @Override
    public Class<? extends DimensionsCraftMenu> getContainerClass()
    {
        return DimensionsCraftMenu.class;
    }

    @Override
    public Optional<MenuType<DimensionsCraftMenu>> getMenuType()
    {
        return Optional.of(DimensionsCraftMenu.Dimensions_Craft_Menu.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType()
    {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(DimensionsCraftMenu container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(container, recipeSlots, maxTransfer, doTransfer, false);
    }




}
