package com.wintercogs.beyonddimensions.integration.module.emi.recipe;

import com.wintercogs.beyonddimensions.api.menu.widget.RecipeFillWidget;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.client.recipe.RecipeFillPlan;
import com.wintercogs.beyonddimensions.client.ui.storage.StorageWorkbenchChoice;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 把 EMI 的配方填进存储界面的一个工作台。切石机、锻造台与铁砧各接自己那一类配方；合成台接其余格数以内的配方
 */
public class NetRecipeHandler<T extends DimensionsNetMenu> implements StandardRecipeHandler<T>
{
    private final ResourceLocation workbench;
    // 这个工作台接的配方类别；合成台为 null。EMI 自带的类别在它自己的插件里才建好，用到时再取
    private final @Nullable Supplier<EmiRecipeCategory> category;
    // 配方的 ID 交给工作台，例如切石机据此选中配方
    private final boolean sendsRecipe;

    private NetRecipeHandler(ResourceLocation workbench, @Nullable Supplier<EmiRecipeCategory> category, boolean sendsRecipe)
    {
        this.workbench = workbench;
        this.category = category;
        this.sendsRecipe = sendsRecipe;
    }

    /**
     * 合成台：格数以内的各种配方，有自己工作台的切石、锻造与铁砧配方除外
     */
    public static <T extends DimensionsNetMenu> NetRecipeHandler<T> crafting()
    {
        return new NetRecipeHandler<>(StorageWorkbenchTypes.CRAFTING, null, false);
    }

    /**
     * 只接 {@code category} 这一类配方的工作台
     */
    public static <T extends DimensionsNetMenu> NetRecipeHandler<T> workbench(Supplier<EmiRecipeCategory> category, ResourceLocation workbench,
                                                                              boolean sendsRecipe)
    {
        return new NetRecipeHandler<>(workbench, category, sendsRecipe);
    }

    private static boolean hasOwnWorkbench(EmiRecipeCategory category)
    {
        return category == VanillaEmiRecipeCategories.STONECUTTING || category == VanillaEmiRecipeCategories.SMITHING
                || category == VanillaEmiRecipeCategories.ANVIL_REPAIRING;
    }

    // 菜单里要填的工作台，这次打开时不能用就是 null
    private @Nullable RecipeFillWidget target(T menu)
    {
        return menu.workbenchWidget(workbench) instanceof RecipeFillWidget widget ? widget : null;
    }

    @Override
    public List<Slot> getInputSources(T menu)
    {
        var slots = new ArrayList<>(menu.slots.subList(menu.inventoryStartIndex, menu.inventoryEndIndex));
        slots.addAll(getCraftingSlots(menu));
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(T menu)
    {
        RecipeFillWidget target = target(menu);
        return target == null ? List.of() : List.copyOf(target.inputSlots());
    }

    // 只有合成台按产物算出合成格里已经放好的份数
    @Override
    public @Nullable Slot getOutputSlot(T menu)
    {
        return category == null ? menu.slots.get(menu.crafting().resultSlot()) : null;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe)
    {
        if (category != null ? recipe.getCategory() != category.get() : hasOwnWorkbench(recipe.getCategory()))
            return false;
        return !recipe.getInputs().isEmpty() && recipe.getInputs().size() <= 9
                && recipe.getInputs().stream().allMatch(input -> input.isEmpty()
                || input.getAmount() > 0 && input.getEmiStacks().stream().anyMatch(stack -> !stack.getItemStack().isEmpty()));
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen)
    {
        return collectInventory(screen.getMenu(), ClientConfigRuntime.recipeViewerEmiNetworkContents);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context)
    {
        // 支持只填入一部分材料；材料方案在真正执行时才计算
        RecipeFillWidget target = target(context.getScreenHandler());
        return active(context.getScreenHandler()) && target != null && supportsRecipe(recipe)
                && recipe.getInputs().size() <= target.inputSlots().size();
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<T> context, List<Widget> widgets, GuiGraphics draw)
    {
        StandardRecipeHandler.renderMissing(recipe, collectInventory(context.getScreenHandler(), true), widgets, draw);
    }

    private EmiPlayerInventory collectInventory(T menu, boolean includeStorage)
    {
        var stacks = new ArrayList<EmiStack>();
        for (Slot slot : getInputSources(menu)) if (slot.hasItem()) stacks.add(EmiStack.of(slot.getItem()));
        if (includeStorage) for (KeyAmount value : menu.storage.getStorage())
            if (!value.isEmpty() && value.key() instanceof ItemStackKey key)
                stacks.add(EmiStack.of(key.getReadOnlyStack(), value.amount()));
        return new EmiPlayerInventory(stacks);
    }

    private boolean active(T menu)
    {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.player.containerMenu == menu && menu.menuSync().hasSnapshot();
    }

    public RecipeFillPlan plan(EmiRecipe recipe, EmiCraftContext<T> context, int batches)
    {
        var pool = RecipeFillPlan.Pool.capture(context.getScreenHandler(), getCraftingSlots(context.getScreenHandler()));
        var inputs = new ArrayList<RecipeFillPlan.Input>();
        for (EmiIngredient ingredient : recipe.getInputs())
        {
            if (ingredient.isEmpty())
            {
                inputs.add(RecipeFillPlan.Input.blank());
                continue;
            }
            var alternatives = ingredient.getEmiStacks().stream().filter(stack -> !stack.getItemStack().isEmpty()).toList();
            var items = alternatives.stream().map(stack -> stack.getItemStack().getItem()).toList();
            var choices = pool.matching(items, key -> alternatives.stream().anyMatch(alt -> alt.isEqual(EmiStack.of(key.getReadOnlyStack()))));
            boolean catalyst = alternatives.size() == 1 && alternatives.getFirst().getRemainder().isEqual(alternatives.getFirst());
            inputs.add(new RecipeFillPlan.Input(choices, ingredient.getAmount(), catalyst));
        }
        return pool.plan(inputs, batches);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context)
    {
        T menu = context.getScreenHandler();
        if (!canCraft(recipe, context) || context.getAmount() < 1) return false;
        int batches = (int) Math.min(Integer.MAX_VALUE, (long) context.getAmount() + existingBatches(recipe, menu));
        RecipeFillPlan fill = plan(recipe, context, batches);
        if (!fill.hasMaterials()) return false;
        if (!menu.commands().recipe(workbench, fill.keys(), fill.amounts(), false, sendsRecipe ? recipe.getId() : null)) return false;
        // 工作区收起或显示着别的工作台时换过来，让玩家看到填好的输入
        StorageWorkbenchChoice.reveal(workbench);
        var mc = Minecraft.getInstance();
        if (!fill.complete())
            mc.player.displayClientMessage(Component.translatable("beyonddimensions.message.insufficient_materials"), true);
        mc.setScreen(context.getScreen());
        return true;
    }

    private int existingBatches(EmiRecipe recipe, T menu)
    {
        Slot result = getOutputSlot(menu);
        if (result == null)
            return 0;
        var output = result.getItem();
        if (output.isEmpty() || recipe.getOutputs().stream().noneMatch(stack -> stack.isEqual(EmiStack.of(output))))
            return 0;
        long batches = Long.MAX_VALUE;
        for (int i = 0; i < 9; i++)
        {
            var stack = menu.slots.get(menu.crafting().gridStart() + i).getItem();
            EmiIngredient input = i < recipe.getInputs().size() ? recipe.getInputs().get(i) : EmiStack.EMPTY;
            if (input.isEmpty())
            {
                if (!stack.isEmpty()) return 0;
                continue;
            }
            if (input.getAmount() <= 0 || input.getEmiStacks().stream().noneMatch(alt -> alt.isEqual(EmiStack.of(stack))))
                return 0;
            batches = Math.min(batches, stack.getCount() / input.getAmount());
        }
        return batches == Long.MAX_VALUE ? 0 : (int) Math.min(Integer.MAX_VALUE, batches);
    }
}
