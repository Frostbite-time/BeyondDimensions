package com.wintercogs.beyonddimensions.integration.module.emi.recipe;

import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.recipe.RecipeFillPlan;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import java.util.*;

public class NetRecipeHandler<T extends DimensionsCraftMenu> implements StandardRecipeHandler<T> {
    @Override public List<Slot> getInputSources(T menu) {
        var slots = new ArrayList<>(menu.slots.subList(menu.inventoryStartIndex, menu.inventoryEndIndex));
        slots.addAll(getCraftingSlots(menu));
        return slots;
    }

    @Override public List<Slot> getCraftingSlots(T menu) {
        return List.copyOf(menu.slots.subList(menu.craftSlotStartIndex, menu.craftSlotEndIndex));
    }

    @Override public Slot getOutputSlot(T menu) { return menu.slots.get(menu.resultSlotIndex); }

    @Override public boolean supportsRecipe(EmiRecipe recipe) {
        return !recipe.getInputs().isEmpty() && recipe.getInputs().size() <= 9
                && recipe.getInputs().stream().allMatch(input -> input.isEmpty()
                || input.getAmount() > 0 && input.getEmiStacks().stream().anyMatch(stack -> !stack.getItemStack().isEmpty()));
    }

    @Override public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        return collectInventory(screen.getMenu(), CommonConfigRuntime.emiAllowNetworkStorageInfo);
    }

    @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        // Partial fills are supported. Build the material plan when invoked, not during every availability query.
        return active(context.getScreenHandler()) && supportsRecipe(recipe);
    }

    @Override public void render(EmiRecipe recipe, EmiCraftContext<T> context, List<Widget> widgets, GuiGraphics draw) {
        StandardRecipeHandler.renderMissing(recipe, collectInventory(context.getScreenHandler(), true), widgets, draw);
    }

    private EmiPlayerInventory collectInventory(T menu, boolean includeStorage) {
        var stacks = new ArrayList<EmiStack>();
        for (Slot slot : getInputSources(menu)) if (slot.hasItem()) stacks.add(EmiStack.of(slot.getItem()));
        if (includeStorage) for (KeyAmount value : menu.storage.getStorage())
            if (!value.isEmpty() && value.key() instanceof ItemStackKey key)
                stacks.add(EmiStack.of(key.getReadOnlyStack(), value.amount()));
        return new EmiPlayerInventory(stacks);
    }

    private boolean active(T menu) {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.player.containerMenu == menu && menu.menuSync().hasSnapshot();
    }

    public RecipeFillPlan plan(EmiRecipe recipe, EmiCraftContext<T> context, int batches) {
        var pool = RecipeFillPlan.Pool.capture(context.getScreenHandler());
        var inputs = new ArrayList<RecipeFillPlan.Input>();
        for (EmiIngredient ingredient : recipe.getInputs()) {
            if (ingredient.isEmpty()) { inputs.add(RecipeFillPlan.Input.blank()); continue; }
            var alternatives = ingredient.getEmiStacks().stream().filter(stack -> !stack.getItemStack().isEmpty()).toList();
            var items = alternatives.stream().map(stack -> stack.getItemStack().getItem()).toList();
            var choices = pool.matching(items, key -> alternatives.stream().anyMatch(alt -> alt.isEqual(EmiStack.of(key.getReadOnlyStack()))));
            boolean catalyst = alternatives.size() == 1 && alternatives.getFirst().getRemainder().isEqual(alternatives.getFirst());
            inputs.add(new RecipeFillPlan.Input(choices, ingredient.getAmount(), catalyst));
        }
        return pool.plan(inputs, batches);
    }

    @Override public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        T menu = context.getScreenHandler();
        if (!active(menu) || !supportsRecipe(recipe) || context.getAmount() < 1) return false;
        int batches = (int)Math.min(Integer.MAX_VALUE, (long)context.getAmount() + existingBatches(recipe, menu));
        RecipeFillPlan fill = plan(recipe, context, batches);
        if (!fill.hasMaterials()) return false;
        if (!menu.commands().recipe(fill.keys(), fill.amounts(), false)) return false;
        var mc = Minecraft.getInstance();
        if (!fill.complete()) mc.player.displayClientMessage(Component.translatable("beyonddimensions.message.insufficient_materials"), true);
        mc.setScreen(context.getScreen());
        return true;
    }

    private int existingBatches(EmiRecipe recipe, T menu) {
        var output = getOutputSlot(menu).getItem();
        if (output.isEmpty() || recipe.getOutputs().stream().noneMatch(stack -> stack.isEqual(EmiStack.of(output)))) return 0;
        long batches = Long.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            var stack = menu.slots.get(menu.craftSlotStartIndex + i).getItem();
            EmiIngredient input = i < recipe.getInputs().size() ? recipe.getInputs().get(i) : EmiStack.EMPTY;
            if (input.isEmpty()) { if (!stack.isEmpty()) return 0; continue; }
            if (input.getAmount() <= 0 || input.getEmiStacks().stream().noneMatch(alt -> alt.isEqual(EmiStack.of(stack)))) return 0;
            batches = Math.min(batches, stack.getCount() / input.getAmount());
        }
        return batches == Long.MAX_VALUE ? 0 : (int)Math.min(Integer.MAX_VALUE, batches);
    }
}
