package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

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
        ForgeHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> nonnulllist = player.level().getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, this.craftSlots, player.level());
        ForgeHooks.setCraftingPlayer(null);

        for (int i = 0; i < nonnulllist.size(); ++i)
        {
            ItemStack slotStack = this.craftSlots.getItem(i);
            ItemStack recipeRemainder = nonnulllist.get(i);

            if (!slotStack.isEmpty())
            {
                int itemsToRemove = 1;

                // 当槽位物品只剩1个时触发特殊逻辑
                if (slotStack.getCount() == 1)
                {
                    ItemStack singleItem = slotStack.copyWithCount(1);
                    boolean consumed = false;

                    // 优先尝试存储系统
                    if (menu.storage != null)
                    {
                        long extracted = menu.storage.extract(new ItemStackType(singleItem), true).getStackAmount();
                        if (extracted >= 1)
                        {
                            if (!player.level().isClientSide())
                            {
                                menu.storage.extract(new ItemStackType(singleItem), false);
                            }
                            itemsToRemove = 0;
                            consumed = true;
                        }
                    }

                    // 存储系统不足时尝试玩家背包
                    if (!consumed)
                    {
                        for (int j = 0; j < player.getInventory().items.size(); j++)
                        {
                            ItemStack invStack = player.getInventory().items.get(j);
                            if (ItemStack.isSameItemSameTags(invStack, singleItem) && invStack.getCount() >= 1)
                            {
                                if (!player.level().isClientSide())
                                {
                                    invStack.shrink(1);
                                    player.getInventory().setItem(j, invStack.isEmpty() ? ItemStack.EMPTY : invStack);
                                }
                                itemsToRemove = 0;
                                consumed = true;
                                break;
                            }
                        }
                    }
                }

                if (itemsToRemove > 0)
                {
                    this.craftSlots.removeItem(i, itemsToRemove);
                }
                slotStack = this.craftSlots.getItem(i);
            }

            // 处理剩余物品
            if (!recipeRemainder.isEmpty())
            {
                if (slotStack.isEmpty())
                {
                    this.craftSlots.setItem(i, recipeRemainder);
                }
                else if (ItemStack.isSameItemSameTags(slotStack, recipeRemainder))
                {
                    recipeRemainder.grow(slotStack.getCount());
                    this.craftSlots.setItem(i, recipeRemainder);
                }
                else if (!this.player.getInventory().add(recipeRemainder))
                {
                    this.player.drop(recipeRemainder, false);
                }
            }
        }

        // 触发合成网格更新
        menu.slotChangedCraftingGrid(menu, player.level(), player, craftSlots, (ResultContainer) this.container, this.index);
    }

}
