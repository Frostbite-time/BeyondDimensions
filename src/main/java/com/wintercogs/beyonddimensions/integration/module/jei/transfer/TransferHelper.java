package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.api.menu.widget.RecipeFillWidget;
import com.wintercogs.beyonddimensions.client.recipe.RecipeFillPlan;
import com.wintercogs.beyonddimensions.client.ui.storage.StorageWorkbenchChoice;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.integration.module.jei.BDjeiPlugin;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class TransferHelper
{
    /**
     * 把 JEI 的配方填进工作台 {@code workbench}：原料按工作台的输入槽依次放，{@code compressOverflow} 时多出格数的原料打包进物质压缩球；
     * {@code recipe} 是配方的 ID，例如切石机据此选中配方
     */
    public static @Nullable IRecipeTransferError transferRecipe(DimensionsNetMenu menu, ResourceLocation workbench, IRecipeSlotsView recipeSlots,
                                                                boolean maxTransfer, boolean doTransfer, boolean compressOverflow,
                                                                @Nullable ResourceLocation recipe)
    {
        var mc = Minecraft.getInstance();
        var runtime = BDjeiPlugin.runtime().orElse(null);
        if (runtime == null || mc.player == null || mc.player.containerMenu != menu || !menu.menuSync().hasSnapshot()
                || !(menu.workbenchWidget(workbench) instanceof RecipeFillWidget target))
            return unavailable();
        var slots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        if (slots.size() > 4096 || !compressOverflow && slots.size() > target.inputSlots().size()) return unavailable();
        var pool = RecipeFillPlan.Pool.capture(menu, target.inputSlots());
        var matcher = runtime.getJeiHelpers().getStackHelper();
        var inputs = new ArrayList<RecipeFillPlan.Input>();
        for (var slot : slots)
        {
            if (slot.isEmpty())
            {
                inputs.add(RecipeFillPlan.Input.blank());
                continue;
            }
            var candidates = slot.getItemStacks().filter(stack -> !stack.isEmpty()).toList();
            var items = candidates.stream().map(stack -> stack.getItem()).toList();
            var choices = pool.matching(items, key -> candidates.stream().anyMatch(candidate -> matcher.isEquivalent(candidate, key.getReadOnlyStack(), UidContext.Recipe)));
            long required = candidates.stream().mapToLong(stack -> Math.max(1, stack.getCount())).max().orElse(1);
            inputs.add(new RecipeFillPlan.Input(choices, required, false));
        }
        var fill = pool.plan(inputs, maxTransfer ? Integer.MAX_VALUE : 1);
        if (doTransfer && fill.hasMaterials())
        {
            if (!menu.commands().recipe(workbench, fill.keys(), fill.amounts(), compressOverflow, recipe)) return unavailable();
            // 工作区收起或显示着别的工作台时换过来，让玩家看到填好的输入
            StorageWorkbenchChoice.reveal(workbench);
        }
        if (!fill.complete()) return new MissStackError(fill.missing().stream().map(slots::get).toList());
        return fill.hasMaterials() ? null : unavailable();
    }

    private static IRecipeTransferError unavailable()
    {
        return new IRecipeTransferError()
        {
            @Override
            public Type getType()
            {
                return Type.INTERNAL;
            }

            @Override
            public void showError(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, IRecipeSlotsView slots, int recipeX, int recipeY)
            {
            }
        };
    }

    private TransferHelper()
    {
    }
}
