package com.wintercogs.beyonddimensions.integration.module.polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.api.common.base.IPolymorphCommon;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过 Polymorph 的公开接口为合成格提供可选配方；合成格内容不变时复用上一次的结果
 */
public final class ComposeRecipeChoices
{
    public record Choice(String id, String label)
    {
    }

    private List<ItemStack> previous = List.of();
    private List<Choice> choices = List.of();
    private Language language;

    public List<Choice> snapshot(DimensionsCraftMenu menu)
    {
        List<ItemStack> items = new ArrayList<>(menu.craftSlotEndIndex - menu.craftSlotStartIndex);
        for (int id = menu.craftSlotStartIndex; id < menu.craftSlotEndIndex; id++)
            items.add(menu.slots.get(id).getItem());
        if (unchanged(items))
            return choices;

        previous = items.stream().map(ItemStack::copy).toList();
        language = Language.getInstance();
        TransientCraftingContainer input = new TransientCraftingContainer(menu, 3, 3,
                NonNullList.of(ItemStack.EMPTY, previous.toArray(ItemStack[]::new)));
        Level level = menu.player.level();
        choices = level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, input, level).stream()
                .map(recipe -> {
                    String result = recipe.assemble(input, level.registryAccess()).getHoverName().getString();
                    return new Choice(recipe.getId().toString(), result + " · " + recipe.getId().getNamespace());
                })
                .toList();
        return choices;
    }

    private boolean unchanged(List<ItemStack> items)
    {
        if (previous.size() != items.size() || language != Language.getInstance())
            return false;
        for (int i = 0; i < items.size(); i++)
        {
            if (!ItemStack.matches(previous.get(i), items.get(i)))
                return false;
        }
        return true;
    }

    public void select(DimensionsCraftMenu menu, String selected)
    {
        if (snapshot(menu).stream().noneMatch(choice -> choice.id().equals(selected)))
            return;
        ResourceLocation id = new ResourceLocation(selected);
        IPolymorphCommon api = PolymorphApi.common();
        menu.player.level().getRecipeManager().byKey(id).ifPresent(recipe ->
                api.getRecipeData(menu.player).ifPresent(data -> data.selectRecipe(recipe)));
        api.getPacketDistributor().sendPlayerRecipeSelectionC2S(id);
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1);
    }
}
