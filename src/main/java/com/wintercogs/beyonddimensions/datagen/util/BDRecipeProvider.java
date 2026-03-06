package com.wintercogs.beyonddimensions.datagen.util;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

public abstract class BDRecipeProvider extends RecipeProvider implements IConditionBuilder
{
    public BDRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public abstract @NotNull String getName();
}
