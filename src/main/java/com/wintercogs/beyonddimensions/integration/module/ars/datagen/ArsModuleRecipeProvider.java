package com.wintercogs.beyonddimensions.integration.module.ars.datagen;

import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ArsModuleRecipeProvider extends BDRecipeProvider
{

    public ArsModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions ArsModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.@NotNull Provider holderLookup)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.ARS_NOUVEAU));

        super.buildRecipes(compatOutput, holderLookup);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ArsModuleBlocks.ARS_SOURCE_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', BlockRegistry.SOURCE_JAR.get())
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_ars_source_pathway", has(BDItems.SPACE_TIME_BAR.get()))
                .save(compatOutput);
    }
}
