package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.refinedmods.refinedstorage.common.content.Blocks;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RSModuleRecipeProvider extends BDRecipeProvider
{

    public RSModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions RSModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.@NotNull Provider holderLookup)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.REFINED_STORAGE));

        super.buildRecipes(compatOutput, holderLookup);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RSModuleBlocks.RS_NET_PATHWAY.get())
                .pattern("ABA")
                .pattern("ACA")
                .pattern("ADA")
                .define('A', com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getQuartzEnrichedIron())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Blocks.INSTANCE.getMachineCasing())
                .define('D', Items.REDSTONE)
                .unlockedBy("unlock_rs_net_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(compatOutput);
    }
}
