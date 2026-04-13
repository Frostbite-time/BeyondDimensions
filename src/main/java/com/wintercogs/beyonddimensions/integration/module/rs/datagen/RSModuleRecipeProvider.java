package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.refinedmods.refinedstorage.common.content.Blocks;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.ModRecipeProvider;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RSModuleRecipeProvider extends BDRecipeProvider
{

    public RSModuleRecipeProvider(HolderLookup.Provider registries, RecipeOutput output)
    {
        super(registries, output);
    }

    @Override
    protected void buildRecipes()
    {
        // TODO uncomment this
//        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.REFINED_STORAGE));

//        super.buildRecipes(compatOutput, holderLookup);

        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, RSModuleBlocks.RS_NET_PATHWAY.get())
                .pattern("ABA")
                .pattern("ACA")
                .pattern("ADA")
                .define('A', com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getQuartzEnrichedIron())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Blocks.INSTANCE.getMachineCasing())
                .define('D', Items.REDSTONE)
                .unlockedBy("unlock_rs_net_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner
    {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
        {
            super(output, lookupProvider);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput output)
        {
            return new ModRecipeProvider(provider, output);
        }

        @Override
        public @NotNull String getName()
        {
            return "BeyondDimensions RSModule Recipe Provider";
        }
    }
}
