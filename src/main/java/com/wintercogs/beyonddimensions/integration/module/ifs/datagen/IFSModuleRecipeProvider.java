package com.wintercogs.beyonddimensions.integration.module.ifs.datagen;

import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ifs.init.IFSModuleItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class IFSModuleRecipeProvider extends BDRecipeProvider
{
    public IFSModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions IFSModule Recipe Provider";
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput, HolderLookup.@NotNull Provider holderLookup)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.INDUSTRIAL_FOREGOING_SOULS));

        super.buildRecipes(compatOutput, holderLookup);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IFSModuleItems.WARDEN_SOUL_TAG_ITEM.get())
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', BDItems.SPACE_TIME_BAR.get())
                .define('C', Items.ECHO_SHARD)
                .unlockedBy("unlock_warden_soul_tag_item", has(Items.ECHO_SHARD))
                .save(compatOutput);
    }
}
