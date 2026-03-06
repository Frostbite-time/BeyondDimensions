package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.integration.module.rs.tags.RSTags;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder
{

    public ModRecipeProvider(PackOutput output)
    {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeOutput)
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

        if (BeyondDimensions.AELoaded)
        {
            // 先把原始 ShapedRecipeBuilder 写好
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDItems.NET_AE_STORAGE_CELL.get())
                    .pattern("ABA")
                    .pattern("BDB")
                    .pattern("CCC")
                    .define('A', appeng.core.definitions.AEBlocks.QUARTZ_GLASS)
                    .define('B', Items.DIAMOND)
                    .define('C', BDItems.SPACE_TIME_BAR.get())
                    .define('D', BDItems.SPACE_TIME_STABLE_FRAME.get())
                    .unlockedBy("unlock_net_ae_storage_cell", has(BDItems.SPACE_TIME_BAR.get()));

            // 用 ConditionalRecipe 包起来，加上“模组已加载”的条件
            ConditionalRecipe.builder()
                    .addCondition(modLoaded(BeyondDimensions.AE2MODID)) // 等同于 forge:mod_loaded
                    .addRecipe(builder::save)                            // 把上面的 ShapedRecipeBuilder 交给它保存
                    // .generateAdvancement() // 可选：需要时让它自己生成 Advancement
                    .build(recipeOutput, ResourceLocation.tryBuild(BDConstants.MODID, "net_ae_storage_cell")); // 最终的配方ID
        }

        if (BeyondDimensions.RS_Loaded)
        {
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.RS_NET_PATHWAY.get())
                    .pattern("ABA")
                    .pattern("ACA")
                    .pattern("ADA")
                    .define('A', RSTags.RS_QUARTZ_ENRICHED_IRON)
                    .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                    .define('C', com.refinedmods.refinedstorage.RSBlocks.MACHINE_CASING.get())
                    .define('D', Items.REDSTONE)
                    .unlockedBy("unlock_rs_net_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()));

            // 用 ConditionalRecipe 包起来，加上“模组已加载”的条件
            ConditionalRecipe.builder()
                    .addCondition(modLoaded(BeyondDimensions.RSModId)) // 等同于 forge:mod_loaded
                    .addRecipe(builder::save)                            // 把上面的 ShapedRecipeBuilder 交给它保存
                    // .generateAdvancement() // 可选：需要时让它自己生成 Advancement
                    .build(recipeOutput, ResourceLocation.tryBuild(BDConstants.MODID, "rs_net_pathway")); // 最终的配方ID
        }

        if (BeyondDimensions.ARS_Loaded)
        {
            // 先把原始 ShapedRecipeBuilder 写好
            ShapedRecipeBuilder builder = ShapedRecipeBuilder
                    .shaped(RecipeCategory.MISC, BDBlocks.ARS_SOURCE_PATHWAY.get())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("ABA")
                    .define('A', BDItems.SPACE_TIME_BAR.get())
                    .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                    .define('C', com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SOURCE_JAR.get())
                    .define('D', Items.ENDER_EYE)
                    .unlockedBy("unlock_ars_source_pathway", has(BDItems.SPACE_TIME_BAR.get()));

            // 用 ConditionalRecipe 包起来，加上“模组已加载”的条件
            ConditionalRecipe.builder()
                    .addCondition(modLoaded(BeyondDimensions.ARS_ModId)) // 等同于 forge:mod_loaded
                    .addRecipe(builder::save)                            // 把上面的 ShapedRecipeBuilder 交给它保存
                    // .generateAdvancement() // 可选：需要时让它自己生成 Advancement
                    .build(recipeOutput, ResourceLocation.tryBuild(BDConstants.MODID, "ars_source_pathway")); // 最终的配方ID
        }

        if (BeyondDimensions.Botania_Loaded)
        {
            // 先把原始 ShapedRecipeBuilder 写好
            ShapedRecipeBuilder builder =
                    ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BDBlocks.MANA_POOL_PATHWAY.get())
                            .pattern("ABA")
                            .pattern("AAA")
                            .define('A', vazkii.botania.common.block.BotaniaBlocks.livingrock)
                            .define('B', BDItems.SPACE_TIME_STABLE_FRAME.get())
                            .unlockedBy("unlock_mana_pool_pathway", has(BDItems.SPACE_TIME_STABLE_FRAME.get()));
            // 用 ConditionalRecipe 包起来，加上“模组已加载”的条件
            ConditionalRecipe.builder()
                    .addCondition(modLoaded(BeyondDimensions.Botania_ModId)) // 等同于 forge:mod_loaded
                    .addRecipe(builder::save)                            // 把上面的 ShapedRecipeBuilder 交给它保存
                    // .generateAdvancement() // 可选：需要时让它自己生成 Advancement
                    .build(recipeOutput, ResourceLocation.tryBuild(BDConstants.MODID, "mana_pool_pathway")); // 最终的配方ID
        }

    }
}
