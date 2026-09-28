package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TerminalUniversalRecipeTransfer implements IUniversalRecipeTransferHandler<DimensionsCraftMenuTerminal>
{


    @Override
    public @NotNull Class<? extends DimensionsCraftMenuTerminal> getContainerClass()
    {
        return DimensionsCraftMenuTerminal.class;
    }

    @Override
    public @NotNull Optional<MenuType<DimensionsCraftMenuTerminal>> getMenuType()
    {
        return Optional.of(DimensionsCraftMenuTerminal.Dimensions_Craft_Menu_Terminal.get());
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(@NotNull DimensionsCraftMenuTerminal container, @NotNull Object recipe,
                                                         @NotNull IRecipeSlotsView recipeSlots, @NotNull Player player,
                                                         boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(container, recipeSlots, maxTransfer, doTransfer, true);
    }


}
