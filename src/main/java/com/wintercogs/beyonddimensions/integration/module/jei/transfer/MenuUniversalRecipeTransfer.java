package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuUniversalRecipeTransfer implements IUniversalRecipeTransferHandler<DimensionsCraftMenu>
{


    @Override
    public @NotNull Class<? extends DimensionsCraftMenu> getContainerClass()
    {
        return DimensionsCraftMenu.class;
    }

    @Override
    public @NotNull Optional<MenuType<DimensionsCraftMenu>> getMenuType()
    {
        return Optional.of(BDMenus.Dimensions_Craft_Menu.get());
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(@NotNull DimensionsCraftMenu container, @NotNull Object recipe,
                                                         @NotNull IRecipeSlotsView recipeSlots, @NotNull Player player,
                                                         boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(getInputSources(container), container.storage.getStorage(), container.player.getInventory().items, recipeSlots, maxTransfer, doTransfer, true);
    }

    private List<Slot> getInputSources(DimensionsCraftMenu menu)
    {
        List<Slot> slots = new ArrayList<>();
        for (int i = menu.craftSlotStartIndex; i < menu.craftSlotEndIndex; i++)
        {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }
}
