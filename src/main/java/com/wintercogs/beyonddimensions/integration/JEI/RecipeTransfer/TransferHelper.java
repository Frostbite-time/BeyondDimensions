package com.wintercogs.beyonddimensions.integration.JEI.RecipeTransfer;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Packet.RecipeFillC2SPacket;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransferHelper
{
    public static @Nullable IRecipeTransferError transferRecipe(List<Slot> inputSource, List<KeyAmount> storage, List<ItemStack> playerInv, IRecipeSlotsView recipeSlots, boolean maxTransfer, boolean doTransfer)
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
            PacketDistributor.sendToServer(new RecipeFillC2SPacket(outKeys, outAmts));
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
