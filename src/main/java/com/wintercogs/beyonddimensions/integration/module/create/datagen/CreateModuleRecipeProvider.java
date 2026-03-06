package com.wintercogs.beyonddimensions.integration.module.create.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class CreateModuleRecipeProvider extends BDRecipeProvider
{
    public CreateModuleRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions CreateModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeOutput)
    {
    }
}
