package com.wintercogs.beyonddimensions.Integration.JEI.RecipeTransfer;

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
    public static @Nullable IRecipeTransferError transferRecipe(List<Slot> inputSource, List<KeyAmount> storage, List<ItemStack> playerInv, IRecipeSlotsView recipeSlots, boolean doTransfer)
    {
        // 1) 聚合可用物品：按 Item -> List<Avail>，避免构造/复制 ItemStack
        final Map<Item, List<Avail>> pool = new HashMap<>();
        final java.util.function.BiConsumer<ItemStackKey, Long> add = (key, amt) -> {
            if (amt == null || amt <= 0) return;
            pool.computeIfAbsent(key.getSource(), i -> new ArrayList<>())
                    .add(new Avail(key, amt));
        };

        // 合成槽物品
        for (Slot slot : inputSource)
        {
            if (slot.hasItem())
            {
                ItemStack s = slot.getItem();
                add.accept(new ItemStackKey(s), (long) s.getCount());
            }
        }
        // 存储槽物品
        for (KeyAmount ka : storage)
        {
            if (ka == null || ka.isEmpty()) continue;
            if (ka.key() instanceof ItemStackKey isk)
            {
                add.accept(isk, ka.amount());
            }
        }
        // 玩家背包
        for (ItemStack s : playerInv)
        {
            if (!s.isEmpty())
            {
                add.accept(new ItemStackKey(s), (long) s.getCount());
            }
        }

        // 2) JEI 输入槽顺序匹配（用 List<ItemStack> 而不是 foreach Stream）
        final List<IRecipeSlotView> missingSlots = new ArrayList<>();
        final ArrayList<IStackKey<?>> outKeys = new ArrayList<>();
        final ArrayList<Long> outAmts = new ArrayList<>();

        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT))
        {
            if (slotView.getRole() != RecipeIngredientRole.INPUT) continue;

            // 把 Stream<ItemStack> 收集成 List<ItemStack>；可反复遍历
            final List<ItemStack> candidates = slotView
                    .getIngredients(VanillaTypes.ITEM_STACK)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toList());

            // 空位 -> 空键 + 0
            if (candidates.isEmpty())
            {
                outKeys.add(EmptyStackKey.INSTANCE);
                outAmts.add(0L);
                continue;
            }

            final int required = requiredCountFor(candidates); // 通常为 1
            boolean satisfied = false;

            for (ItemStack alt : candidates)
            {
                final Item item = alt.getItem();
                final List<Avail> list = pool.get(item);
                if (list == null || list.isEmpty()) continue;

                long available = 0L;
                for (Avail a : list) available += Math.max(0L, a.remain);
                if (available < required) continue;

                // 取一个代表性 Key（带组件信息）
                ItemStackKey repKey = null;
                for (Avail a : list)
                {
                    if (a.remain > 0)
                    {
                        repKey = a.key;
                        break;
                    }
                }
                if (repKey == null) continue;

                // 扣减池
                int left = required;
                for (Avail a : list)
                {
                    if (left <= 0) break;
                    long take = Math.min(a.remain, left);
                    if (take > 0)
                    {
                        a.remain -= take;
                        left -= (int) take;
                    }
                }

                outKeys.add(repKey);
                outAmts.add((long) required);
                satisfied = true;
                break;
            }

            if (!satisfied)
            {
                outKeys.add(EmptyStackKey.INSTANCE);
                outAmts.add(0L);
                missingSlots.add(slotView);
            }
        }

        // 3) 发包（顺序即 JEI 槽位顺序；空位保留）
        if (doTransfer)
        {
            PacketDistributor.sendToServer(new RecipeFillC2SPacket(outKeys, outAmts));
        }

        // 4) 返回缺失错误
        if (!missingSlots.isEmpty())
        {
            return new MissStackError(missingSlots);
        }
        return null;
    }

    /**
     * 该槽位需求数量：默认取候选堆叠中最大的 count（典型为1）
     */
    private static int requiredCountFor(List<ItemStack> candidates)
    {
        int max = 0;
        for (ItemStack s : candidates)
        {
            if (s != null) max = Math.max(max, s.getCount());
        }
        return Math.max(1, max);
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
