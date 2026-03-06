package com.wintercogs.beyonddimensions.integration.module.ars.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ars.init.ArsModuleBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class ArsModuleRecipeProvider extends BDRecipeProvider
{
    public ArsModuleRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions ArsModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeOutput)
    {
        var compatOut = withConditions(recipeOutput, modLoaded(OtherModIds.ARS_NOUVEAU));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ArsModuleBlocks.ARS_SOURCE_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SOURCE_JAR.get())
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_ars_source_pathway", has(BDItems.SPACE_TIME_BAR.get()))
                .save(compatOut, BeyondDimensions.makeId("ars_source_pathway"));
    }
}
