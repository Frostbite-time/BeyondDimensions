package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.network.packet.c2s.RecipeFillC2SPacket;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransferHelper
{
    /**
     * 处理配方转移的主要方法，其他方法均为辅助方法
     * <p>从存储、背包、以及其他可用源获取可用物，随后计算向合成槽填入的信息（包括填充物和数量），之后发往服务端，从服务端执行物品转移的实际逻辑
     * <p>对于每一个槽位而言，如果有多个可选材料，总是选用总量剩余最多的那一个
     */
    public static @Nullable IRecipeTransferError transferRecipe(List<Slot> inputSource, List<KeyAmount> storage, List<ItemStack> playerInv, IRecipeSlotsView recipeSlots, boolean maxTransfer, boolean doTransfer, boolean compressOverflow)
    {
        final Map<Item, List<Avail>> pool = new HashMap<>();

        for (Slot slot : inputSource)
        {
            if (slot.hasItem())
            {
                ItemStack s = slot.getItem();
                addAvail(pool, new ItemStackKey(s), s.getCount());
            }
        }

        for (KeyAmount ka : storage)
        {
            if (ka == null || ka.isEmpty()) continue;
            if (ka.key() instanceof ItemStackKey isk)
            {
                addAvail(pool, isk, ka.amount());
            }
        }

        for (ItemStack s : playerInv)
        {
            if (!s.isEmpty())
            {
                addAvail(pool, new ItemStackKey(s), s.getCount());
            }
        }

        final List<IRecipeSlotView> missingSlots = new ArrayList<>();
        final ArrayList<IStackKey<?>> outKeys = new ArrayList<>();
        final ArrayList<Long> outAmts = new ArrayList<>();
        boolean hasMissing = false;

        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT))
        {
            if (slotView.getRole() != RecipeIngredientRole.INPUT) continue;

            final List<ItemStack> candidates = slotView
                    .getIngredients(VanillaTypes.ITEM_STACK)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toList());

            if (candidates.isEmpty())
            {
                outKeys.add(EmptyStackKey.INSTANCE);
                outAmts.add(0L);
                continue;
            }

            final long required = requiredCountFor(candidates);
            boolean satisfied = false;

            for (ItemStack alt : candidates)
            {
                final Item item = alt.getItem();
                final List<Avail> list = pool.get(item);
                if (list == null || list.isEmpty()) continue;

                for (Avail avail : list)
                {
                    if (avail.remain <= 0) continue;

                    long available = avail.remain;
                    if (available < required) continue;

                    consume(avail, required);
                    outKeys.add(avail.key);
                    outAmts.add(required);
                    satisfied = true;
                    break;
                }

                if (satisfied)
                {
                    break;
                }
            }

            if (!satisfied)
            {
                hasMissing = true;
                outKeys.add(EmptyStackKey.INSTANCE);
                outAmts.add(0L);
                missingSlots.add(slotView);
            }
        }

        if (doTransfer)
        {
            ClientPacketDistributor.sendToServer(new RecipeFillC2SPacket(outKeys, outAmts, compressOverflow));
        }

        if (hasMissing)
        {
            return new MissStackError(missingSlots);
        }
        return null;
    }

    /**
     * 该槽位需求数量：默认取候选堆叠中最大的 count（典型为 1）
     */
    private static long requiredCountFor(List<ItemStack> candidates)
    {
        int max = 0;
        for (ItemStack s : candidates)
        {
            if (s != null) max = Math.max(max, s.getCount());
        }
        return Math.max(1, max);
    }

    private static void addAvail(Map<Item, List<Avail>> pool, ItemStackKey key, long amount)
    {
        if (amount <= 0) return;
        pool.computeIfAbsent(key.getSource(), i -> new ArrayList<>()).add(new Avail(key, amount));
    }

    private static void consume(Avail avail, long amount)
    {
        long take = Math.min(avail.remain, amount);
        if (take > 0)
        {
            avail.remain -= take;
        }
    }

    /**
     * 可用条目：仅 Key + 可用数量；不创建/复制 ItemStack
     */
    private static final class Avail
    {
        final ItemStackKey key;
        long remain;

        Avail(ItemStackKey key, long remain)
        {
            this.key = key;
            this.remain = remain;
        }
    }
}
