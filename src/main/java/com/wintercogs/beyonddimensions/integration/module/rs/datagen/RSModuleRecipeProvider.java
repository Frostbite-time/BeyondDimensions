package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.refinedmods.refinedstorage.RSBlocks;
import com.refinedmods.refinedstorage.RSItems;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class RSModuleRecipeProvider extends BDRecipeProvider
{
    public RSModuleRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions RSModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeOutput)
    {
        var compatOut = withConditions(recipeOutput, modLoaded(OtherModIds.REFINED_STORAGE));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RSModuleBlocks.RS_NET_PATHWAY.get())
                .pattern("ABA")
                .pattern("ACA")
                .pattern("ADA")
                .define('A', RSItems.QUARTZ_ENRICHED_IRON.get())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', RSBlocks.MACHINE_CASING.get())
                .define('D', Items.REDSTONE)
                .unlockedBy("unlock_rs_net_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(compatOut, BeyondDimensions.makeId("rs_net_pathway"));
    }
}
