package com.wintercogs.beyonddimensions.datagen;

import appeng.core.definitions.AEBlocks;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.datagen.util.BDRecipeProvider;
import com.wintercogs.beyonddimensions.integration.module.rs.Tags.RSTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.common.block.BotaniaBlocks;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends BDRecipeProvider
{

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions Recipe Provider";
    }

    @Override
    protected void buildRecipes(@NotNull RecipeOutput recipeOutput)
    {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.UNSTABLE_SPACE_TIME_FRAGMENT.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.DIAMOND)
                .define('B', Items.TNT)
                .define('C', Items.NETHER_STAR)
                .unlockedBy("unlock_net_creater", has(Items.NETHER_STAR))
                .save(recipeOutput);

        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get()),
                        RecipeCategory.MISC,
                        BDItems.SPACE_TIME_BAR.get(),
                        1f,
                        600)
                .unlockedBy("unlock_space_time_bar", has(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_CREATER.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ACA")
                .define('A', Items.NETHERITE_INGOT)
                .define('B', Items.ENDER_EYE)
                .define('C', Items.ENDER_PEARL)
                .define('D', BDItems.STABLE_SPACE_TIME_FRAGMENT.get())
                .unlockedBy("unlock_net_creater", has(BDItems.STABLE_SPACE_TIME_FRAGMENT.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.SPACE_TIME_STABLE_FRAME.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', Items.REDSTONE)
                .define('C', Items.ENDER_EYE)
                .unlockedBy("unlock_space_time_stable_frame", has(BDItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_MEMBER_INVITER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.IRON_INGOT)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_member_inviter", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_MANAGER_INVITER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.GOLD_INGOT)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_manager_inviter", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.ENDER_PEARL)
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_net_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_INTERFACE.get())
                .pattern("ABA")
                .pattern("CDE")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.PISTON)
                .define('D', Items.REDSTONE_TORCH)
                .define('E', Items.STICKY_PISTON)
                .unlockedBy("unlock_net_interface", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_ENERGY_PATHWAY.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .define('C', Items.COPPER_INGOT)
                .define('D', Items.ENDER_EYE)
                .unlockedBy("unlock_net_energy_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_CONTROL.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.COMPARATOR)
                .define('C', Items.REPEATER)
                .define('D', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_control", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_TERMINAL_ITEM.get())
                .pattern("ABA")
                .pattern("BDB")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.GOLD_INGOT)
                .define('D', BDItems.NET_MEMBER_INVITER.get())
                .unlockedBy("unlock_net_terminal_item", has(BDItems.NET_MEMBER_INVITER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_TERMINAL_BLOCK.get())
                .pattern("ACA")
                .pattern("BDB")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.GOLD_INGOT)
                .define('C', Items.CRAFTING_TABLE)
                .define('D', BDItems.NET_MEMBER_INVITER.get())
                .unlockedBy("unlock_net_terminal_item", has(BDItems.NET_MEMBER_INVITER.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_GIFTER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.DIAMOND)
                .define('B', Items.GOLD_INGOT)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_gifter", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_DESTROYER.get())
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" B ")
                .define('A', Items.TNT)
                .define('B', Items.GOLD_INGOT)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_destroyer", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', Items.IRON_INGOT)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_dimensionsal_connect_block", has(BDItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_FURNACE_BLOCK.get())
                .pattern("AAA")
                .pattern("BCB")
                .pattern("ADA")
                .define('A', Items.COBBLESTONE)
                .define('B', Items.PISTON)
                .define('C', BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .define('D', Items.REDSTONE_TORCH)
                .unlockedBy("unlock_net_furnace_block", has(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_PUMP_BLOCK.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', Items.COBBLESTONE)
                .define('B', Items.STICKY_PISTON)
                .define('C', BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .unlockedBy("unlock_net_pump_block", has(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.NET_HOPPER_BLOCK.get())
                .pattern("   ")
                .pattern("ABC")
                .pattern("DDD")
                .define('A', Items.BUCKET)
                .define('B', BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get())
                .define('C', Items.HOPPER)
                .define('D', Items.COBBLESTONE)
                .unlockedBy("unlock_net_hopper_block", has(BDBlocks.DIMENSIONAL_CONNECT_BLOCK.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_MAGNET_ITEM.get())
                .pattern(" AB")
                .pattern("A C")
                .pattern(" AB")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', Items.IRON_INGOT)
                .define('C', BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get())
                .unlockedBy("unlock_net_magnet_item", has(BDItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_FEEDER_ITEM.get())
                .pattern(" AA")
                .pattern("ABC")
                .pattern(" AA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', Items.APPLE)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_feeder_item", has(BDItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_RESTOCKER_ITEM.get())
                .pattern(" AA")
                .pattern("ABC")
                .pattern(" AA")
                .define('A', BDItems.SPACE_TIME_BAR.get())
                .define('B', Items.CHEST)
                .define('C', BDItems.SPACE_TIME_STABLE_FRAME.get())
                .unlockedBy("unlock_net_restocker_item", has(BDItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.XP_EXCHANGE_ITEM.get())
                .pattern("  A")
                .pattern(" B ")
                .pattern("C  ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .define('C', BDItems.SPACE_TIME_BAR.get())
                .unlockedBy("unlock_xp_exchange_item", has(BDItems.SPACE_TIME_BAR.get()))
                .save(recipeOutput);



        if (BeyondDimensions.RS_Loaded)
        {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.RS_NET_PATHWAY.get())
                    .pattern("ABA")
                    .pattern("ACA")
                    .pattern("ADA")
                    .define('A', RSTags.RS_QUARTZ_ENRICHED_IRON)
                    .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                    .define('C', Blocks.INSTANCE.getMachineCasing())
                    .define('D', Items.REDSTONE)
                    .unlockedBy("unlock_rs_net_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(BeyondDimensions.RSModId)));
        }

        if (BeyondDimensions.IFS_Loaded)
        {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.WARDEN_SOUL_TAG_ITEM.get())
                    .pattern("AAA")
                    .pattern("BCB")
                    .pattern("AAA")
                    .define('A', Items.AMETHYST_SHARD)
                    .define('B', BDItems.SPACE_TIME_BAR.get())
                    .define('C', Items.ECHO_SHARD)
                    .unlockedBy("unlock_warden_soul_tag_item", has(Items.ECHO_SHARD))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(BeyondDimensions.IFS_ModId)));
        }

        if (BeyondDimensions.ARS_Loaded)
        {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.ARS_SOURCE_PATHWAY.get())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("ABA")
                    .define('A', BDItems.SPACE_TIME_BAR.get())
                    .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                    .define('C', BlockRegistry.SOURCE_JAR.get())
                    .define('D', Items.ENDER_EYE)
                    .unlockedBy("unlock_ars_source_pathway", has(BDItems.SPACE_TIME_BAR.get()))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(BeyondDimensions.ARS_ModId)));
        }

        if (BeyondDimensions.Botania_Loaded)
        {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.MANA_POOL_PATHWAY.get())
                    .pattern("ABA")
                    .pattern("AAA")
                    .define('A', BotaniaBlocks.livingrock)
                    .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                    .unlockedBy("unlock_mana_pool_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()))
                    .save(recipeOutput.withConditions(new ModLoadedCondition(BeyondDimensions.Botania_ModId)));
        }

    }
}
