package com.wintercogs.beyonddimensions.Integration.EMI.Recipe;

import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class NetRecipeHandler implements StandardRecipeHandler<DimensionsCraftMenu>
{

    @Override
    public List<Slot> getInputSources(DimensionsCraftMenu handler)
    {
        List<Slot> inputSlots = new ArrayList<>();
        for(Slot slot : handler.slots)
        {
            if(!(slot instanceof ResultSlot) && !(slot instanceof StoredStackSlot))
            {
                inputSlots.add(slot);
            }
        }
        return inputSlots;
    }

    @Override
    public List<Slot> getCraftingSlots(DimensionsCraftMenu handler)
    {
        List<Slot> craftingSlots = new ArrayList<>();
        for(int i = handler.slots.size() - 1; i >= handler.slots.size() - 9; --i)
        {
            craftingSlots.add(handler.slots.get(i));
        }
        return craftingSlots;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe)
    {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<DimensionsCraftMenu> screen)
    {
        return new EmiPlayerInventory(getInputSources(screen.getMenu()).stream().map(Slot::getItem).map(EmiStack::of).toList());
//        List<EmiStack> stacks = getInputSources(screen.getMenu()).stream().map(Slot::getItem).map(EmiStack::of).collect(Collectors.toCollection(ArrayList::new));
//        if(screen.getMenu().storage.getStorage() != null)
//        {
//            for(IStackType stackType : screen.getMenu().storage.getStorage())
//            {
//                if(stackType instanceof ItemStackType itemStackType)
//                {
//                    if(!itemStackType.isEmpty())
//                        stacks.add(EmiStack.of(itemStackType.getStack()));
//                }
//            }
//        }
//        return new EmiPlayerInventory(stacks);
    }

}
