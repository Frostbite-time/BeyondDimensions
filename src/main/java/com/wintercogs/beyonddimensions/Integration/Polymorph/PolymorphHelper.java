package com.wintercogs.beyonddimensions.Integration.Polymorph;

import com.illusivesoulworks.polymorph.common.crafting.RecipeSelection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class PolymorphHelper
{
    public static Optional<CraftingRecipe> getRecipe(Player player, RecipeType<CraftingRecipe> crafting, CraftingContainer input, Level level) {
        return RecipeSelection.getPlayerRecipe(player.containerMenu, crafting, input, level, player);
    }
}
