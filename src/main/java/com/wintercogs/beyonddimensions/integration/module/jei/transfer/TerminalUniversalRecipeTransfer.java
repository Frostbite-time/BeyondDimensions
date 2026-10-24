package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetTerminalMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TerminalUniversalRecipeTransfer implements IUniversalRecipeTransferHandler<DimensionsNetTerminalMenu>
{


    @Override
    public @NotNull Class<? extends DimensionsNetTerminalMenu> getContainerClass()
    {
        return DimensionsNetTerminalMenu.class;
    }

    @Override
    public @NotNull Optional<MenuType<DimensionsNetTerminalMenu>> getMenuType()
    {
        return Optional.of(BDMenus.Dimensions_Net_Terminal_Menu.get());
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(@NotNull DimensionsNetTerminalMenu container, @NotNull Object recipe,
                                                         @NotNull IRecipeSlotsView recipeSlots, @NotNull Player player,
                                                         boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(container, StorageWorkbenchTypes.CRAFTING, recipeSlots, maxTransfer, doTransfer, true, null);
    }


}
