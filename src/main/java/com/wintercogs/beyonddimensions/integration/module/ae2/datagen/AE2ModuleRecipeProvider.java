package com.wintercogs.beyonddimensions.integration.module.ae2.datagen;

import appeng.core.definitions.AEBlocks;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static net.neoforged.neoforge.common.conditions.NeoForgeConditions.modLoaded;

public class AE2ModuleRecipeProvider extends BDRecipeProvider
{
    protected AE2ModuleRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput)
    {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes()
    {
        RecipeOutput compatOutput = this.output.withConditions(modLoaded(OtherModIds.AE2));

        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, AE2ModuleItems.NET_AE_STORAGE_CELL)
                .pattern("ABA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A', AEBlocks.QUARTZ_GLASS)
                .define('B', Items.DIAMOND)
                .define('C', BDItems.SPACE_TIME_BAR)
                .define('D', BDItems.SPACE_TIME_STABLE_FRAME)
                .unlockedBy("unlock_net_ae_storage_cell", has(BDItems.SPACE_TIME_BAR))
                .save(compatOutput);
    }
}
