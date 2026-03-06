package com.wintercogs.beyonddimensions.integration.module.create.datagen;

import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CreateModuleRecipeProvider extends BDRecipeProvider
{

    public CreateModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions CreateModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.@NotNull Provider holderLookup)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.REFINED_STORAGE));

        super.buildRecipes(compatOutput, holderLookup);


    }
}
