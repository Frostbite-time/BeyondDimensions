package com.wintercogs.beyonddimensions.Integration.JEI;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Packet.RecipeFillC2SPacket;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecipeTransferHandler implements IRecipeTransferHandler<DimensionsCraftMenu, RecipeHolder<CraftingRecipe>>
{


    public RecipeTransferHandler()
    {
    }

    @Override
    public Class<? extends DimensionsCraftMenu> getContainerClass()
    {
        return DimensionsCraftMenu.class;
    }

    @Override
    public Optional<MenuType<DimensionsCraftMenu>> getMenuType()
    {
        return Optional.of(DimensionsCraftMenu.Dimensions_Craft_Menu.get());
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType()
    {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(DimensionsCraftMenu container, RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
    {
        // 1. 获取配方输入信息
        // 1. 从JEI的视图获取完整配方输入（保留空位）
        List<Ingredient> ingredients = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            if (slotView.getRole() == RecipeIngredientRole.INPUT) {
                // 将槽位中的多个选项合并成一个复合Ingredient
                Ingredient merged = Ingredient.of(slotView.getIngredients(VanillaTypes.ITEM_STACK));
                ingredients.add(merged != null ? merged : Ingredient.EMPTY);
            }
        }
        List<ItemStack> inputElements = new ArrayList<>();

        // 2. 收集所有可用物品来源（合成槽+存储槽）
        List<Slot> craftingSlots = getInputSources(container);
        List<IStackType> storageSlots = container.storage.getStorage();
        List<ItemStack> availableItems = new ArrayList<>();

        // 收集合成槽物品
        for (Slot slot : craftingSlots) {
            if (slot.hasItem()) {
                availableItems.add(slot.getItem().copy());
            }
        }

        // 收集存储槽物品
        for (IStackType stackType : storageSlots) {
            if (stackType instanceof ItemStackType itemStackType) {
                ItemStack stack = itemStackType.getStack();
                if (!stack.isEmpty()) {
                    availableItems.add(stack.copy());
                }
            }
        }

        // 收集背包物品
        for(ItemStack itemStack : container.player.getInventory().items)
        {
            if(!itemStack.isEmpty())
                availableItems.add(itemStack.copy());
        }

        // 3. 创建虚拟库存用于模拟扣除
        List<ItemStack> virtualInventory = new ArrayList<>();
        for (ItemStack stack : availableItems) {
            virtualInventory.add(stack.copy());
        }

        // 4. 匹配配方需求
        boolean hasEnoughMaterials = true;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                inputElements.add(ItemStack.EMPTY);
                continue;
            }

            int required = 1; // 原版合成配方每个原料需要1个
            List<ItemStack> matching = new ArrayList<>();

            // 在虚拟库存中寻找匹配项
            for (ItemStack stack : virtualInventory) {
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    matching.add(stack);
                }
            }

            // 计算可用数量
            int available = matching.stream().mapToInt(ItemStack::getCount).sum();
            if (available >= required) {
                // 创建合并堆栈并扣除库存
                ItemStack merged = matching.isEmpty() ? ItemStack.EMPTY :
                        new ItemStack(matching.get(0).getItem(), required);
                inputElements.add(merged);

                int remaining = required;
                for (ItemStack stack : matching) {
                    int deduct = Math.min(remaining, stack.getCount());
                    stack.shrink(deduct);
                    remaining -= deduct;
                    if (remaining <= 0) break;
                }
            } else {
                hasEnoughMaterials = false;
                break;
            }
        }

        // 5. 处理材料不足情况
        if (!hasEnoughMaterials) {
            if (doTransfer) {
                player.displayClientMessage(
                        Component.translatable("beyonddimensions.message.insufficient_materials"),
                        true
                );
            }
            return new IRecipeTransferError()
            {
                @Override
                public Type getType()
                {
                    return Type.USER_FACING;
                }
            };
        }

        // 6. 执行实际转移
        if (doTransfer) {
            // 发送网络包到服务端处理物品移动
            PacketDistributor.sendToServer(new RecipeFillC2SPacket(inputElements));
        }

        return null; // 返回null表示无错误
    }

    // 获取合成输入槽位（需根据实际容器实现）
    private List<Slot> getInputSources(DimensionsCraftMenu menu) {
        List<Slot> slots = new ArrayList<>();
        for (int i = menu.craftSlotStartIndex; i < menu.craftSlotEndIndex; i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }



}
