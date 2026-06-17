package com.wintercogs.beyonddimensions.util;

import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class InventoryHelper
{
    /**
     * 尝试将传入的物品堆叠插入玩家背包，会修改传入的堆叠。
     * <p>返回的堆叠与传入堆叠为同一引用。
     *
     * @param player 玩家
     * @param stack  传入的堆叠，会被修改
     * @return 余量
     */
    public static ItemStack transferToPlayerInventory(Player player, ItemStack stack)
    {
        Inventory inventory = player.getInventory();
        int maxStackSize = stack.getMaxStackSize();

        // 第一阶段：合并已有堆叠
        for (int i = 0; i < 36 && !stack.isEmpty(); i++)
        {
            ItemStack slotStack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(slotStack, stack))
            {
                int transferable = Math.min(maxStackSize - slotStack.getCount(), stack.getCount());
                if (transferable > 0)
                {
                    slotStack.grow(transferable);
                    stack.shrink(transferable);
                    inventory.setItem(i, slotStack);
                }
            }
        }

        // 第二阶段：填充空槽
        if (!stack.isEmpty())
        {
            for (int i = 0; i < 36 && !stack.isEmpty(); i++)
            {
                if (inventory.getItem(i).isEmpty())
                {
                    ItemStack cloned = stack.copy();
                    cloned.setCount(Math.min(stack.getCount(), maxStackSize));
                    inventory.setItem(i, cloned);
                    stack.shrink(cloned.getCount());
                }
            }
        }

        return stack;
    }

    /**
     * 尝试从玩家背包提取指定数量的物品堆叠，传入堆叠不会被修改。
     * <p>返回的堆叠与传入的堆叠是不同的实例</p>
     *
     * @param player 玩家
     * @param stack  目标物品堆，其count即为目标数量
     */
    public static ItemStack extractFromPlayerInventory(Player player, ItemStack stack)
    {
        int extract = 0;
        int aim = stack.getCount();
        Inventory inventory = player.getInventory();

        // 遍历背包主槽位（0-35）
        for (int i = 0; i < 36 && extract < aim; i++)
        {
            ItemStack invStack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(invStack, stack))
            {
                int tryExtract = Math.min(aim - extract, invStack.getCount());
                invStack.shrink(tryExtract);
                extract += tryExtract;
                inventory.setItem(i, invStack.isEmpty() ? ItemStack.EMPTY : invStack);
            }
        }
        return stack.copyWithCount(extract);
    }

    /**
     * 给定一个物品，从玩家的背包以及其curios仓库中找到一个对应的物品堆并返回
     */
    @Nullable
    public static ItemStack findItemInPlayerInventory(Player player, Item item)
    {
        ItemStack itemStack = null;
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(item))
            itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        else if (player.getItemInHand(InteractionHand.OFF_HAND).is(item))
            itemStack = player.getItemInHand(InteractionHand.OFF_HAND);
        else
        {
            ItemStack findInInv = player.getInventory().getNonEquipmentItems().stream()
                    .filter(stack -> stack.is(item))
                    .findFirst().orElse(null);
            if (findInInv != null)
                itemStack = findInInv;
        }

        if (itemStack == null && ModPresence.isLoaded(OtherModIds.CURIOS))
        {
//            itemStack = CuriosApi.getCuriosInventory(player)
//                    .flatMap(iCuriosItemHandler ->
//                            iCuriosItemHandler.findFirstCurio(stack -> stack.is(item))
//                    )
//                    .map(SlotResult::stack)
//                    .orElse(null);
        }

        return itemStack;
    }
}
