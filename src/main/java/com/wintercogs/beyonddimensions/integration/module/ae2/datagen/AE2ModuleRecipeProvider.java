package com.wintercogs.beyonddimensions.integration.module.ae2.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class AE2ModuleRecipeProvider extends BDRecipeProvider
{
    public AE2ModuleRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions AE2Module Recipe Provider";
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> recipeOutput)
    {
        var compatOut = withConditions(recipeOutput, modLoaded(OtherModIds.AE2));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AE2ModuleItems.NET_AE_STORAGE_CELL.get())
                .pattern("ABA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A', appeng.core.definitions.AEBlocks.QUARTZ_GLASS)
                .define('B', Items.DIAMOND)
                .define('C', BDItems.SPACE_TIME_BAR.get())
                .define('D', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_ae_storage_cell", has(BDItems.SPACE_TIME_BAR.get()))
                .save(compatOut, BeyondDimensions.makeId("net_ae_storage_cell"));
    }
}
