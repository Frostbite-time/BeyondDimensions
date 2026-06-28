package com.wintercogs.beyonddimensions.integration.module.botania.datagen;

import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.botania.init.BotaniaModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.concurrent.CompletableFuture;

public class BotaniaModuleRecipeProvider extends BDRecipeProvider
{

    public BotaniaModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions BotaniaModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.@NotNull Provider holderLookup)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.BOTANIA));

        super.buildRecipes(compatOutput, holderLookup);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BotaniaModuleBlocks.MANA_POOL_PATHWAY.get())
                .pattern("ABA")
                .pattern("AAA")
                .define('A', BotaniaBlocks.LIVINGROCK)
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_mana_pool_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(compatOutput);

    }
}
