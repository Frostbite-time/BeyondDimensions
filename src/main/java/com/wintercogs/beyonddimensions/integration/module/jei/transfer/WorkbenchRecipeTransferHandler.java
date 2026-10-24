package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 把一类 JEI 配方填进存储界面的一个工作台，例如切石机配方填进切石机。存储菜单与终端菜单各注册一份
 *
 * @param <C> 存储菜单的类型
 * @param <R> 配方的类型
 */
public class WorkbenchRecipeTransferHandler<C extends DimensionsNetMenu, R> implements IRecipeTransferHandler<C, R>
{
    private final Class<C> containerClass;
    private final Supplier<MenuType<C>> menuType;
    private final RecipeType<R> recipeType;
    private final ResourceLocation workbench;
    private final Function<R, @Nullable ResourceLocation> recipeId;

    /**
     * @param recipeId 配方的 ID，工作台按它选中配方时给出，例如切石机；不需要时返回 null
     */
    public WorkbenchRecipeTransferHandler(Class<C> containerClass, Supplier<MenuType<C>> menuType, RecipeType<R> recipeType,
                                          ResourceLocation workbench, Function<R, @Nullable ResourceLocation> recipeId)
    {
        this.containerClass = containerClass;
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.workbench = workbench;
        this.recipeId = recipeId;
    }

    @Override
    public Class<? extends C> getContainerClass()
    {
        return containerClass;
    }

    @Override
    public Optional<MenuType<C>> getMenuType()
    {
        return Optional.of(menuType.get());
    }

    @Override
    public RecipeType<R> getRecipeType()
    {
        return recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(C container, R recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
    {
        return TransferHelper.transferRecipe(container, workbench, recipeSlots, maxTransfer, doTransfer, false, recipeId.apply(recipe));
    }
}
