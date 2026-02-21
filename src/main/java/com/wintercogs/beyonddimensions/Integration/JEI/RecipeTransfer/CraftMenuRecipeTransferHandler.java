package com.wintercogs.beyonddimensions.Integration.JEI.RecipeTransfer;

import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftMenuRecipeTransferHandler implements IRecipeTransferHandler<DimensionsCraftMenu, CraftingRecipe>
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
        return Optional.of(UIRegister.Dimensions_Craft_Menu.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType()
    {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(DimensionsCraftMenu container, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(getInputSources(container), container.storage.getStorage(), container.player.getInventory().items, recipeSlots, maxTransfer, doTransfer);
    }

    // 获取合成输入槽位（需根据实际容器实现）
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
