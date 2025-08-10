package com.wintercogs.beyonddimensions.Datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.ModBlocks;
import com.wintercogs.beyonddimensions.Integration.RS.Tags.RSTags;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_TERMINAL_ITEM.get())
                .pattern("ABA")
                .pattern("BDB")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.GOLD_INGOT)
                .define('D', ModItems.NET_MEMBER_INVITER.get())
                .unlockedBy("unlock_net_terminal_item", has(ModItems.NET_MEMBER_INVITER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_TERMINAL_BLOCK.get())
                .pattern("ACA")
                .pattern("BDB")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.GOLD_INGOT)
                .define('C', Items.CRAFTING_TABLE)
                .define('D', ModItems.NET_MEMBER_INVITER.get())
                .unlockedBy("unlock_net_terminal_item", has(ModItems.NET_MEMBER_INVITER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_GIFTER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.DIAMOND)
                .define('B', Items.GOLD_INGOT)
                .define('C', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_gifter", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_DESTROYER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.TNT)
                .define('B', Items.GOLD_INGOT)
                .define('C', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_destroyer", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_AE_STORAGE_CELL.get())
                .pattern("ABA")
                .pattern("BDB")
                .pattern("CCC")
                .define('A', Items.GLASS)
                .define('B', Items.DIAMOND)
                .define('C', ModItems.SPACE_TIME_BAR.get())
                .define('D', ModItems.NET_MANAGER_INVITER.get())
                .unlockedBy("unlock_net_ae_storage_cell", has(ModItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', ModItems.SPACE_TIME_BAR.get())
                .define('B', Items.IRON_INGOT)
                .define('C', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_dimensionsal_connect_block", has(ModItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_FURNACE_BLOCK.get())
                .pattern("AAA")
                .pattern("BCB")
                .pattern("ADA")
                .define('A', Items.COBBLESTONE)
                .define('B', Items.PISTON)
                .define('C', ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .define('D', Items.REDSTONE_TORCH)
                .unlockedBy("unlock_net_furnace_block", has(ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_PUMP_BLOCK.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.COBBLESTONE)
                .define('B', Items.STICKY_PISTON)
                .define('C', ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .unlockedBy("unlock_net_pump_block", has(ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NET_HOPPER_BLOCK.get())
                .pattern("   ")
                .pattern("ABC")
                .pattern("DDD")
                .define('A', Items.BUCKET)
                .define('B', ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .define('C', Items.HOPPER)
                .define('D', Items.COBBLESTONE)
                .unlockedBy("unlock_net_hopper_block", has(ModBlocks.DIMENSIONAL_CONNECT_BLOCK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_MAGNET_ITEM.get())
                .pattern(" AB")
                .pattern("A C")
                .pattern(" AB")
                .define('A', ModItems.SPACE_TIME_BAR.get())
                .define('B', Items.IRON_INGOT)
                .define('C', ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get())
                .unlockedBy("unlock_net_magnet_item", has(ModItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NET_FEEDER_ITEM.get())
                .pattern(" AA")
                .pattern("ABC")
                .pattern(" AA")
                .define('A', ModItems.SPACE_TIME_BAR.get())
                .define('B', Items.APPLE)
                .define('C', ModItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_feeder_item", has(ModItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.XP_EXCHANGE_ITEM.get())
                .pattern("  A")
                .pattern(" B ")
                .pattern("C  ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .define('C', ModItems.SPACE_TIME_BAR.get())
                .unlockedBy("unlock_xp_exchange_item", has(ModItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        if(BeyondDimensions.RS_Loaded)
        {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RS_NET_PATHWAY.get())
                    .pattern("ABA")
                    .pattern("ACA")
                    .pattern("ADA")
                    .define('A', RSTags.RS_QUARTZ_ENRICHED_IRON)
                    .define('B', ModItems.SPACE_TIME_STABLE_FRAME.get())
                    .define('C', com.refinedmods.refinedstorage.common.content.Blocks.INSTANCE.getMachineCasing())
                    .define('D', Items.REDSTONE)
                    .unlockedBy("unlock_rs_net_pathway", has(ModItems.SPACE_TIME_STABLE_FRAME.get()))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(BeyondDimensions.RSModId)));
        }

        if(BeyondDimensions.IFS_Loaded)
        {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WARDEN_SOUL_TAG_ITEM.get())
                    .pattern("AAA")
                    .pattern("BCB")
                    .pattern("AAA")
                    .define('A', Items.AMETHYST_SHARD)
                    .define('B', ModItems.SPACE_TIME_BAR.get())
                    .define('C', Items.ECHO_SHARD)
                    .unlockedBy("unlock_warden_soul_tag_item", has(Items.ECHO_SHARD))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(BeyondDimensions.IFS_ModId)));
        }

    }
}
