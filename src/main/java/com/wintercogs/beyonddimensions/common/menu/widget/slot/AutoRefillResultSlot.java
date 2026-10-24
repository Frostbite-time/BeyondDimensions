package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.widget.CraftingWidget;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;

public class AutoRefillResultSlot extends ResultSlot
{

    private final CraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final Player player;
    private final CraftingWidget crafting;

    public AutoRefillResultSlot(CraftingWidget crafting, Player player, CraftingContainer craftSlots, ResultContainer resultSlots, int slot, int xPosition, int yPosition)
    {
        super(player, craftSlots, resultSlots, slot, xPosition, yPosition);
        this.crafting = crafting;
        this.player = player;
        this.craftSlots = craftSlots;
        this.resultSlots = resultSlots;
    }

    public AutoRefillResultSlot(CraftingWidget crafting, Player player, CraftingContainer craftSlots, ResultContainer resultSlots, int slot)
    {
        this(crafting, player, craftSlots, resultSlots, slot, 0, 0);
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack craftedStack)
    {
        if (player.level().isClientSide()) return;

        // 发放进度时原版会清掉产物槽记录的配方，剩余物要按这份配方算，先取出来
        Recipe<?> recipe = resultSlots.getRecipeUsed();
        this.checkTakeAchievements(craftedStack);

        ForgeHooks.setCraftingPlayer(player);
        NonNullList<ItemStack> remainingItems = crafting.remainingItems(recipe, craftSlots);
        ForgeHooks.setCraftingPlayer(null);

        crafting.consumeIngredients(() -> consumeIngredients(player, remainingItems));
    }

    // 扣除一次合成的原料并放回剩余物
    private void consumeIngredients(Player player, NonNullList<ItemStack> remainingItems)
    {
        // 一般而言，此函数每次调用最多完成一次合成
        int craftTimes = 1;

        for (int slotIndex = 0; slotIndex < remainingItems.size(); ++slotIndex)
        {
            ItemStack slotStack = this.craftSlots.getItem(slotIndex);
            ItemStack recipeRemainder = remainingItems.get(slotIndex);

            if (!slotStack.isEmpty())
            {
                int itemsToRemove = craftTimes;

                // 如果完成本次合成，会导致合成槽槽位原料耗尽，我们优先从其他地方取
                if (slotStack.getCount() <= itemsToRemove)
                {
                    ItemStackKey toRemoveKey = new ItemStackKey(slotStack);

                    // 优先尝试存储系统
                    int extracted = (int) crafting.storage().extract(toRemoveKey, itemsToRemove, false, false).amount();
                    itemsToRemove -= extracted;

                    // 存储系统不足时尝试玩家背包
                    for (int i = 0; i < player.getInventory().items.size() && itemsToRemove > 0; i++)
                    {
                        ItemStack invStack = player.getInventory().items.get(i);
                        if (ItemStack.isSameItemSameTags(invStack, toRemoveKey.getReadOnlyStack()))
                        {
                            int shrinkAmount = Math.min(itemsToRemove, invStack.getCount());
                            invStack.shrink(shrinkAmount);
                            player.getInventory().items.set(i, invStack.isEmpty() ? ItemStack.EMPTY : invStack);
                            itemsToRemove -= shrinkAmount;
                        }
                    }
                }

                // 如果此时itemsToRemove仍然大于0，消耗槽位物品，
                // 由于槽位是先验原料量再显示产物的机制，所以此处必然能补全最后剩余的开销
                if (itemsToRemove > 0)
                {
                    this.craftSlots.removeItem(slotIndex, itemsToRemove);
                }
                slotStack = this.craftSlots.getItem(slotIndex);
            }

            // 如果有原料返回物，则填充
            if (!recipeRemainder.isEmpty())
            {
                // 返回物的量，必然等于次数（也许可能会有某些时候一个物品返回多个产物？不过我还没见过）
                // 况且目前有些受限，getRemainingItemsFor返回的count受到输入量影响，不能直接用
                // 如果后续出现问题再改
                int remainderCount = craftTimes;
                ItemStackKey remainderKey = new ItemStackKey(recipeRemainder);

                // 首先往槽位最多填充一个
                if (slotStack.isEmpty() && remainderCount > 0)
                {
                    this.craftSlots.setItem(slotIndex, remainderKey.copyStackWithCount(1));
                    remainderCount--;
                }
                // 继续填入存储
                if (remainderCount > 0)
                {
                    remainderCount = (int) crafting.storage().insert(remainderKey, remainderCount, false).amount();
                }
                // 随后填充玩家背包
                if (remainderCount > 0)
                {
                    ItemStack insertStack = remainderKey.copyStackWithCount(remainderCount);
                    this.player.getInventory().add(insertStack);
                    remainderCount = insertStack.getCount();
                }
                // 如果仍然有剩余，直接掉落
                if (remainderCount > 0)
                {
                    this.player.drop(remainderKey.copyStackWithCount(remainderCount), false);
                }
            }
        }
    }
}
