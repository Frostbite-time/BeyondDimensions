package com.wintercogs.beyonddimensions.integration.module.ae2.datagen;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.ae2.init.AE2ModuleItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.ConditionalRecipe;

import java.util.function.Consumer;

public class AE2ModuleRecipeProvider extends BDRecipeProvider
{
    public AE2ModuleRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<net.minecraft.data.recipes.FinishedRecipe> recipeOutput)
    {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AE2ModuleItems.NET_AE_STORAGE_CELL.get())
                .pattern("ABA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A', appeng.core.definitions.AEBlocks.QUARTZ_GLASS)
                .define('B', Items.DIAMOND)
                .define('C', BDItems.SPACE_TIME_BAR.get())
                .define('D', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_ae_storage_cell", has(BDItems.SPACE_TIME_BAR.get()));

        ConditionalRecipe.builder()
                .addCondition(modLoaded(OtherModIds.AE2))
                .addRecipe(builder::save)
                .build(recipeOutput, ResourceLocation.tryBuild(BDConstants.MODID, "net_ae_storage_cell"));
    }
}
