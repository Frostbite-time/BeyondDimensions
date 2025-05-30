package com.wintercogs.beyonddimensions.Integration.Polymorph;

import com.illusivesoulworks.polymorph.client.recipe.widget.PlayerRecipesWidget;
import com.wintercogs.beyonddimensions.GUI.DimensionsCraftGUI;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

public class RecipeWidget extends PlayerRecipesWidget
{
    private final DimensionsCraftMenu menu;
    public RecipeWidget(DimensionsCraftGUI<?> containerScreen, Slot outputSlot)
    {
        super(containerScreen, outputSlot);
        menu = containerScreen.getMenu();
    }

    @Override
    public void selectRecipe(ResourceLocation resourceLocation)
    {
        super.selectRecipe(resourceLocation);

        // 不能理解为什么这么做，但是别人也这么写
        Minecraft.getInstance().level.getRecipeManager().byKey(resourceLocation).ifPresent(recipe -> {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick((this.menu).containerId, 1);
        });
    }
}
