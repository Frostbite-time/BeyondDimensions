package com.wintercogs.beyonddimensions.integration.ae2.datagen;

import appeng.core.definitions.AEBlocks;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Datagen.helpers.BaseRecipeProvider;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.ae2.init.AE2ModuleItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AE2ModuleRecipeProvider extends BaseRecipeProvider
{
    public AE2ModuleRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "AE2 Module Recipe";
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput, @NotNull HolderLookup.Provider holderLookup)
    {
        RecipeOutput compatOutput = recipeOutput.withConditions(modLoaded(OtherModIds.AE2));

        super.buildRecipes(compatOutput, holderLookup);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AE2ModuleItems.NET_AE_STORAGE_CELL.get())
                .pattern("ABA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A', AEBlocks.QUARTZ_GLASS)
                .define('B', Items.DIAMOND)
                .define('C', ModItems.SPACE_TIME_BAR.get())
                .define('D', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_ae_storage_cell", has(ModItems.SPACE_TIME_BAR.get()))
                .save(compatOutput);
    }
}
