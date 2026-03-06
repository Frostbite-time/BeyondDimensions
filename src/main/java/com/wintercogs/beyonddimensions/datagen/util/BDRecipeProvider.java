package com.wintercogs.beyonddimensions.datagen.util;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class BDRecipeProvider extends RecipeProvider implements IConditionBuilder
{
    public BDRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public abstract @NotNull String getName();

    protected static Consumer<FinishedRecipe> withConditions(Consumer<FinishedRecipe> consumer, ICondition... conditions)
    {
        if (conditions == null || conditions.length == 0)
        {
            return consumer;
        }

        return recipe -> {
            ConditionalRecipe.Builder builder = ConditionalRecipe.builder();
            for (ICondition condition : conditions)
            {
                builder.addCondition(condition);
            }

            builder.addRecipe(out -> out.accept(recipe))
                    .build(consumer, recipe.getId());
        };
    }
}
