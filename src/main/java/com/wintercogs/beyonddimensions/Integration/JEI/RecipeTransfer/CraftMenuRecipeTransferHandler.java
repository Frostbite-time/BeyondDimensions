package com.wintercogs.beyonddimensions.Integration.JEI.RecipeTransfer;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.RecipeFillC2SPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CraftMenuRecipeTransferHandler implements IRecipeTransferHandler<DimensionsCraftMenu, CraftingRecipe>
{
    public CraftMenuRecipeTransferHandler()
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
        return Optional.of(UIRegister.Dimensions_Craft_Menu.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType()
    {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public IRecipeTransferError transferRecipe(DimensionsCraftMenu container, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer)
    {
        // 首先收集所有物品信息-------------------------------------------------------------------------------------------------

        // 收集所有可用物品来源（合成槽+存储槽）
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


        // 匹配配方需求并构建列表------------------------------------------------------------------------------------------------------
        List<IRecipeSlotView> missingSlots = new ArrayList<>(); // 记录缺失材料的槽位，仅记录缺失部分
        List<ItemStack> inputElements = new ArrayList<>(); // 记录输入的原料，包括空气

        for(IRecipeSlotView slotView: recipeSlots.getSlotViews(RecipeIngredientRole.INPUT))
        {
            if(slotView.getRole() == RecipeIngredientRole.INPUT)
            {
                // 将多个选项合并为一个复合Ingredient
                Ingredient mergedIngredient = Ingredient.of(slotView.getIngredients(VanillaTypes.ITEM_STACK));

                // 实际检测

                // 空位处理
                if(mergedIngredient.isEmpty())
                {
                    inputElements.add(ItemStack.EMPTY);
                    continue;
                }
                // 检查仓库物品
                int required = 1; // 原版材料在每个合成槽需要一个
                List<ItemStack> matching = new ArrayList<>();
                for (ItemStack stack : availableItems) { // 统计全部可用物
                    if (!stack.isEmpty() && mergedIngredient.test(stack)) {
                        matching.add(stack);
                    }
                }

                // 计算可用数量
                int available = matching.stream().mapToInt(ItemStack::getCount).sum();
                if (available >= required) {
                    // 创建合并堆栈并扣除库存
                    ItemStack merged = matching.isEmpty() ? ItemStack.EMPTY :
                            matching.get(0).copyWithCount(required); //因为仅需要一个，所以使用第一个即可
                    inputElements.add(merged);

                    int remaining = required;
                    for (ItemStack stack : matching) {
                        int deduct = Math.min(remaining, stack.getCount());
                        stack.shrink(deduct);
                        remaining -= deduct;
                        if (remaining <= 0) break;
                    }
                } else {
                    inputElements.add(ItemStack.EMPTY); // 材料不足的时候添加空位
                    missingSlots.add(slotView);
                }

            }
        }

        // 处理结果----------------------------------------------------------------------------------------------------------
        if(doTransfer) //无论如何，允许执行转移操作，即使材料不足
        {
            PacketRegister.INSTANCE.sendToServer(new RecipeFillC2SPacket(inputElements));
        }
        if(!missingSlots.isEmpty())
        {
            return new MissStackError(missingSlots);
        }
        return null;

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
