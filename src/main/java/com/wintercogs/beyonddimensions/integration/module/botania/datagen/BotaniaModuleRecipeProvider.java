package com.wintercogs.beyonddimensions.integration.module.botania.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.function.Consumer;

public class BotaniaModuleRecipeProvider extends BDRecipeProvider
{
    public BotaniaModuleRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions BotaniaModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeOutput)
    {
        var compatOut = withConditions(recipeOutput, modLoaded(OtherModIds.BOTANIA));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BotaniaModuleBlocks.MANA_POOL_PATHWAY.get())
                .pattern("ABA")
                .pattern("AAA")
                .define('A', BotaniaBlocks.livingrock)
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_mana_pool_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(compatOut, BeyondDimensions.makeId("mana_pool_pathway"));
    }
}
