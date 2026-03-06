package com.wintercogs.beyonddimensions.datagen.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

public abstract class BDRecipeProvider extends RecipeProvider
{
    protected BDRecipeProvider(HolderLookup.Provider registries, RecipeOutput output)
    {
        super(registries, output);
    }
}
