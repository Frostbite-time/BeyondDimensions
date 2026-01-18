package com.wintercogs.beyonddimensions.Integration.Polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class PolymorphHelper
{
    public static Optional<RecipeHolder<CraftingRecipe>> getRecipe(Player player, RecipeType<CraftingRecipe> crafting, CraftingInput input, Level level)
    {
        return PolymorphApi.getInstance().getRecipeManager().getPlayerRecipe(player.containerMenu, crafting, input, level, player);
    }
}
