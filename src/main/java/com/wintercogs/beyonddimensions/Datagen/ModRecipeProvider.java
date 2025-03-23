package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder
{

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput)
    {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.UNSTABLE_SPACE_TIME_FRAGMENT.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.DIAMOND)
                .define('B', Items.TNT)
                .define('C', Items.NETHER_STAR)
                .unlockedBy("unlock_net_creater", has(Items.NETHER_STAR))
                .save(recipeOutput);

        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get()),
                RecipeCategory.MISC,
                ModItems.SPACE_TIME_BAR.get(),
                1f,
                600)
                .unlockedBy("unlock_space_time_bar", has(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_CREATER.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ACA")
                .define('A', Items.NETHERITE_INGOT)
                .define('B', Items.ENDER_EYE)
                .define('C', Items.ENDER_PEARL)
                .define('D', ModItems.STABLE_SPACE_TIME_FRAGMENT.get())
                .unlockedBy("unlock_net_creater", has(ModItems.STABLE_SPACE_TIME_FRAGMENT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPACE_TIME_STABLE_FRAME.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', ModItems.SPACE_TIME_BAR.get())
                .define('B', Items.REDSTONE)
                .define('C', Items.ENDER_EYE)
                .unlockedBy("unlock_space_time_stable_frame", has(ModItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_MEMBER_INVITER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.IRON_INGOT)
                .define('C', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_member_inviter", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_MANAGER_INVITER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.GOLD_INGOT)
                .define('C', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_manager_inviter", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModItems.SPACE_TIME_BAR.get())
                .define('B', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.ENDER_PEARL)
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_net_pathway", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_INTERFACE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.PISTON)
                .define('D', Items.REDSTONE_TORCH)
                .define('E', Items.STICKY_PISTON)
                .unlockedBy("unlock_net_interface", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_ENERGY_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModItems.SPACE_TIME_BAR.get())
                .define('B', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.COPPER_INGOT)
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_net_energy_pathway", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_CONTROL.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.COMPARATOR)
                .define('C', Items.REPEATER)
                .define('D', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_control", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);





    }
}
