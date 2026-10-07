package com.wintercogs.beyonddimensions.integration.module.rs.datagen;

import com.refinedmods.refinedstorage.common.content.Blocks;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.rs.init.RSModuleBlocks;
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

public class RSModuleRecipeProvider extends BDRecipeProvider
{

    public RSModuleRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput)
    {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes()
    {
        RecipeOutput compatOutput = this.output.withConditions(modLoaded(OtherModIds.REFINED_STORAGE));

        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, RSModuleBlocks.RS_NET_PATHWAY.get())
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
