package com.wintercogs.beyonddimensions.integration.module.ae2lt.datagen;

import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2lt.init.AE2LTModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AE2LTModuleRecipeProvider extends BDRecipeProvider
{
    public AE2LTModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions AE2LT Module Recipe Provider";
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.@NotNull Provider provider)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.AE2_LIGHTNING_TECH));
        super.buildRecipes(compatOutput, provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AE2LTModuleBlocks.LIGHTNING_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.LIGHTNING_ROD)
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_lightning_pathway", has(BDItems.SPACE_TIME_BAR.get()))
                .save(compatOutput);
    }
}
