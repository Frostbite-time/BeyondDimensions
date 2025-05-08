package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;

public class AutoRefillResultSlot extends ResultSlot
{

    private final CraftingContainer craftSlots;
    private final Player player;
    private DimensionsCraftMenu menu;

    public AutoRefillResultSlot(DimensionsCraftMenu menu, Player player, CraftingContainer craftSlots, Container container, int slot, int xPosition, int yPosition)
    {
        super(player, craftSlots, container, slot, xPosition, yPosition);
        this.menu = menu;
        this.player = player;
        this.craftSlots = craftSlots;
    }

    @Override
    public void onTake(Player player, ItemStack stack)
    {
        this.checkTakeAchievements(stack);
        CraftingInput.Positioned positionedInput = this.craftSlots.asPositionedCraftInput();
        CraftingInput craftingGrid = positionedInput.input();
        int gridStartX = positionedInput.left();
        int gridStartY = positionedInput.top();

        CommonHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remainingItems = player.level().getRecipeManager()
                .getRemainingItemsFor(RecipeType.CRAFTING, craftingGrid, player.level());
        CommonHooks.setCraftingPlayer(null);

        for (int gridRow = 0; gridRow < craftingGrid.height(); gridRow++) {
            for (int gridCol = 0; gridCol < craftingGrid.width(); gridCol++) {
                int slotIndex = gridCol + gridStartX + (gridRow + gridStartY) * this.craftSlots.getWidth();
                ItemStack slotStack = this.craftSlots.getItem(slotIndex);
                ItemStack recipeRemainder = remainingItems.get(gridCol + gridRow * craftingGrid.width());

                if (!slotStack.isEmpty()) {
                    int itemsToRemove = 1;

                    if (slotStack.getCount() == 1) {
                        ItemStack singleItem = slotStack.copyWithCount(1);
                        boolean consumed = false;

                        // 优先尝试存储系统
                        long extracted = menu.storage.extract(new ItemStackType(singleItem), true).getStackAmount();
                        if (extracted >= 1) {
                            if (!player.level().isClientSide()) {
                                menu.storage.extract(new ItemStackType(singleItem), false);
                            }
                            itemsToRemove = 0;
                            consumed = true;
                        }

                        // 存储系统不足时尝试玩家背包
                        if (!consumed) {
                            for (int i = 0; i < player.getInventory().items.size(); i++) {
                                ItemStack invStack = player.getInventory().items.get(i);
                                if (ItemStack.isSameItemSameComponents(invStack, singleItem) && invStack.getCount() >= 1) {
                                    if (!player.level().isClientSide()) {
                                        invStack.shrink(1);
                                        player.getInventory().setItem(i, invStack.isEmpty() ? ItemStack.EMPTY : invStack);
                                    }
                                    itemsToRemove = 0;
                                    consumed = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (itemsToRemove > 0) {
                        this.craftSlots.removeItem(slotIndex, itemsToRemove);
                    }

                    slotStack = this.craftSlots.getItem(slotIndex);
                }


                if (!recipeRemainder.isEmpty()) {
                    if (slotStack.isEmpty()) {
                        this.craftSlots.setItem(slotIndex, recipeRemainder);
                    } else if (ItemStack.isSameItemSameComponents(slotStack, recipeRemainder)) {
                        recipeRemainder.grow(slotStack.getCount());
                        this.craftSlots.setItem(slotIndex, recipeRemainder);
                    } else if (!this.player.getInventory().add(recipeRemainder)) {
                        this.player.drop(recipeRemainder, false);
                    }
                }
            }
        }
        menu.slotChangedCraftingGrid(menu, player.level(), player, craftSlots, (ResultContainer) this.container, null, this.index);



    }
}
