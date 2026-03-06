package com.wintercogs.beyonddimensions.datagen.util;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

public abstract class BDRecipeProvider extends RecipeProvider implements IConditionBuilder
{
    public BDRecipeProvider(PackOutput output)
    {
        super(output);
    }
}
