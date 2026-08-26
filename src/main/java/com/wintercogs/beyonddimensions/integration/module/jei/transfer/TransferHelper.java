package com.wintercogs.beyonddimensions.integration.module.jei.transfer;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDPackets;
import com.wintercogs.beyonddimensions.network.packet.c2s.RecipeFillC2SPacket;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
        final ArrayList<TransferPlan> plans = new ArrayList<>();
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
                plans.add(TransferPlan.empty());
                continue;
            }

            final long required = requiredCountFor(candidates);
            AvailableGroup selected = findBestAvailable(candidates, pool, required);
            boolean satisfied = selected != null;

            if (satisfied)
            {
                consume(pool.get(selected.key.getSource()), selected.key, required);
                plans.add(new TransferPlan(selected.key, required));
            }

            if (!satisfied)
            {
                hasMissing = true;
                plans.add(TransferPlan.empty());
                missingSlots.add(slotView);
            }
        }

        long transferMultiplier = hasMissing ? 1 : getTransferMultiplier(plans, pool, maxTransfer);
        final ArrayList<IStackKey<?>> outKeys = new ArrayList<>(plans.size());
        final ArrayList<Long> outAmts = new ArrayList<>(plans.size());
        for (TransferPlan plan : plans)
        {
            outKeys.add(plan.key);
            outAmts.add(plan.required * transferMultiplier);
        }

        if (doTransfer)
        {
            BDPackets.INSTANCE.sendToServer(new RecipeFillC2SPacket(outKeys, outAmts, compressOverflow));
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

    private static @Nullable AvailableGroup findBestAvailable(List<ItemStack> candidates, Map<Item, List<Avail>> pool, long required)
    {
        AvailableGroup best = null;
        final ArrayList<AvailableGroup> groups = new ArrayList<>();
        final Set<Item> scannedItems = new HashSet<>();

        for (ItemStack alt : candidates)
        {
            final Item item = alt.getItem();
            if (!scannedItems.add(item)) continue;

            final List<Avail> list = pool.get(item);
            if (list == null || list.isEmpty()) continue;

            for (Avail avail : list)
            {
                if (avail.remain <= 0) continue;
                addAvailable(groups, avail.key, avail.remain);
            }
        }

        for (AvailableGroup group : groups)
        {
            if (group.remaining < required) continue;
            if (best == null || group.remaining > best.remaining)
            {
                best = group;
            }
        }

        return best;
    }

    private static void addAvailable(List<AvailableGroup> groups, ItemStackKey key, long remaining)
    {
        for (AvailableGroup group : groups)
        {
            if (group.key.isSameTypeSameComponents(key))
            {
                group.remaining += remaining;
                return;
            }
        }
        groups.add(new AvailableGroup(key, remaining));
    }

    private static long getTransferMultiplier(List<TransferPlan> plans, Map<Item, List<Avail>> pool, boolean maxTransfer)
    {
        if (!maxTransfer) return 1;

        final ArrayList<RequiredGroup> requiredGroups = new ArrayList<>();
        long multiplier = Long.MAX_VALUE;
        boolean hasMaterial = false;

        for (TransferPlan plan : plans)
        {
            if (plan.isEmpty()) continue;

            hasMaterial = true;
            addRequired(requiredGroups, plan.itemKey, plan.required);
            long maxBySlot = Math.max(1, plan.itemKey.getVanillaMaxStackSize() / plan.required);
            multiplier = Math.min(multiplier, maxBySlot);
        }

        if (!hasMaterial) return 1;

        for (RequiredGroup group : requiredGroups)
        {
            long available = getAvailable(pool, group.key);
            multiplier = Math.min(multiplier, available / group.required);
        }

        return Math.max(1, multiplier == Long.MAX_VALUE ? 1 : multiplier);
    }

    private static void addRequired(List<RequiredGroup> groups, ItemStackKey key, long required)
    {
        for (RequiredGroup group : groups)
        {
            if (group.key.isSameTypeSameComponents(key))
            {
                group.required += required;
                return;
            }
        }
        groups.add(new RequiredGroup(key, required));
    }

    private static long getAvailable(Map<Item, List<Avail>> pool, ItemStackKey key)
    {
        long available = 0;
        List<Avail> entries = pool.get(key.getSource());
        if (entries == null) return 0;

        for (Avail avail : entries)
        {
            if (avail.key.isSameTypeSameComponents(key))
            {
                available += avail.amount;
            }
        }
        return available;
    }

    private static void addAvail(Map<Item, List<Avail>> pool, ItemStackKey key, long amount)
    {
        if (amount <= 0) return;
        pool.computeIfAbsent(key.getSource(), i -> new ArrayList<>()).add(new Avail(key, amount));
    }

    private static void consume(@Nullable List<Avail> entries, ItemStackKey key, long amount)
    {
        if (entries == null || amount <= 0) return;

        long remaining = amount;
        for (Avail avail : entries)
        {
            if (remaining <= 0) return;
            if (!avail.key.isSameTypeSameComponents(key)) continue;

            long take = Math.min(avail.remain, remaining);
            if (take > 0)
            {
                avail.remain -= take;
                remaining -= take;
            }
        }
    }

    private static final class TransferPlan
    {
        final IStackKey<?> key;
        final ItemStackKey itemKey;
        final long required;

        TransferPlan(ItemStackKey key, long required)
        {
            this.key = key;
            this.itemKey = key;
            this.required = required;
        }

        static TransferPlan empty()
        {
            return new TransferPlan(EmptyStackKey.INSTANCE, null, 0);
        }

        private TransferPlan(IStackKey<?> key, @Nullable ItemStackKey itemKey, long required)
        {
            this.key = key;
            this.itemKey = itemKey;
            this.required = required;
        }

        boolean isEmpty()
        {
            return itemKey == null || key.isEmpty() || required <= 0;
        }
    }

    private static final class AvailableGroup
    {
        final ItemStackKey key;
        long remaining;

        AvailableGroup(ItemStackKey key, long remaining)
        {
            this.key = key;
            this.remaining = remaining;
        }
    }

    private static final class RequiredGroup
    {
        final ItemStackKey key;
        long required;

        RequiredGroup(ItemStackKey key, long required)
        {
            this.key = key;
            this.required = required;
        }
    }

    /**
     * 可用条目：仅 Key + 可用数量；不创建/复制 ItemStack
     */
    private static final class Avail
    {
        final ItemStackKey key;
        final long amount;
        long remain;

        Avail(ItemStackKey key, long amount)
        {
            this.key = key;
            this.amount = amount;
            this.remain = amount;
        }
    }
}
