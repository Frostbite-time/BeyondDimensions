package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.InsertPlan;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.UnifiedStorageRules;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MatterCompressionBall extends Item
{
    private static final ResourceLocation UNCOMPRESS_RULE_ID = BeyondDimensions.makeId("uncompress_matter_ball");

    /**
     * 物质球最多套这么多层，球里不装球算一层。套得太深的物品存档读不出来
     */
    public static final int MAX_NESTING = 16;

    public MatterCompressionBall(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    public static boolean hasIStackList(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("stack_list", Tag.TAG_LIST) &&
                !tag.getList("stack_list", Tag.TAG_COMPOUND).isEmpty();
    }

    public static List<KeyAmount> getIStackList(ItemStack stack)
    {
        List<KeyAmount> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("stack_list", Tag.TAG_LIST))
            return result;

        ListTag listTag = tag.getList("stack_list", Tag.TAG_COMPOUND);
        for (Tag element : listTag)
        {
            CompoundTag elementTag = (CompoundTag) element;
            KeyAmount stackType = KeyAmount.deserializeNBT(elementTag);
            result.add(stackType);
        }
        return result;
    }

    public static void setIStackList(ItemStack stack, List<KeyAmount> stackList)
    {
        ListTag listTag = new ListTag();
        for (KeyAmount stackType : stackList)
        {
            CompoundTag elementTag = KeyAmount.serializeNBT(stackType);
            listTag.add(elementTag);
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.put("stack_list", listTag);
    }

    /**
     * 物质球进入维度网络时拆成内容物，球里的球一并拆开；空球直接吞掉
     */
    public static void registerStorageRule()
    {
        UnifiedStorageRules.insertMatching(UNCOMPRESS_RULE_ID, MatterCompressionBall::isBall, request -> {
            Map<IStackKey<?>, Long> contents = new LinkedHashMap<>();
            try
            {
                collectContents((ItemStackKey) request.key(), 1L, contents);
            }
            catch (ArithmeticException overflow)
            {
                return InsertPlan.reject();
            }
            InsertPlan plan = InsertPlan.convert(1L);
            contents.forEach(plan::add);
            return plan;
        });
    }

    // 判断是否为物质球
    private static boolean isBall(IStackKey<?> key)
    {
        return key instanceof ItemStackKey itemKey && itemKey.getSource() instanceof MatterCompressionBall;
    }

    /**
     * 这种资源能否装进物质球：已经套到 {@link #MAX_NESTING} 层的物质球不能再装
     */
    public static boolean canBePacked(IStackKey<?> key)
    {
        return nesting(key, MAX_NESTING) < MAX_NESTING;
    }

    // 物质球套了多少层，不是物质球为 0；最多数到 limit 层
    private static int nesting(IStackKey<?> key, int limit)
    {
        if (limit <= 0 || !isBall(key))
            return 0;
        List<KeyAmount> entries = getIStackList(((ItemStackKey) key).getReadOnlyStack());
        int inner = 0;
        for (KeyAmount entry : entries)
        {
            if (entry != null && !entry.isEmpty())
                inner = Math.max(inner, nesting(entry.key(), limit - 1));
        }
        return inner + 1;
    }

    // 递归地收集物质球内容
    private static void collectContents(ItemStackKey ball, long count, Map<IStackKey<?>, Long> out)
    {
        List<KeyAmount> entries = getIStackList(ball.getReadOnlyStack());
        for (KeyAmount entry : entries)
        {
            if (entry == null || entry.isEmpty())
                continue;
            long amount = Math.multiplyExact(entry.amount(), count);
            if (isBall(entry.key()))
                collectContents((ItemStackKey) entry.key(), amount, out);
            else
                out.merge(entry.key(), amount, Math::addExact);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack ballStack = player.getItemInHand(usedHand);
        List<KeyAmount> contents = getIStackList(ballStack);
        if (contents.isEmpty())
        {
            return InteractionResultHolder.pass(ballStack);
        }

        if (!level.isClientSide())
        {
            boolean transferToNetwork = player.isShiftKeyDown();
            DimensionsNet primaryNet = transferToNetwork ? DimensionsNet.getPrimaryNetFromPlayer(player) : null;
            UnifiedStorage networkStorage = primaryNet == null ? null : primaryNet.getUnifiedStorage();
            List<KeyAmount> remainingContents = new ArrayList<>();
            boolean changed = false;

            for (KeyAmount content : contents)
            {
                if (content == null || content.isEmpty()) continue;

                IStackKey<?> remainingKey = content.key();
                long remainingAmount = content.amount();

                if (remainingKey instanceof ItemStackKey itemKey)
                {
                    remainingAmount = transferItemToInventory(player, itemKey, remainingAmount);
                }

                if (networkStorage != null && remainingAmount > 0L)
                {
                    KeyAmount remaining = networkStorage.insert(remainingKey, remainingAmount, false);
                    remainingKey = remaining.key();
                    remainingAmount = remaining.amount();
                }

                if (remainingAmount > 0L && !remainingKey.isEmpty())
                {
                    remainingContents.add(new KeyAmount(remainingKey, remainingAmount));
                }
                if (remainingAmount != content.amount()
                        || !remainingKey.isSameTypeSameComponents(content.key()))
                {
                    changed = true;
                }
            }

            if (changed)
            {
                if (remainingContents.isEmpty())
                {
                    ballStack.shrink(1);
                }
                else
                {
                    setIStackList(ballStack, remainingContents);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(ballStack, level.isClientSide());
    }

    private static long transferItemToInventory(Player player, ItemStackKey key, long amount)
    {
        long remaining = Math.max(0L, amount);
        while (remaining > 0L)
        {
            int request = (int) Math.min(Integer.MAX_VALUE, remaining);
            ItemStack leftover = InventoryHelper.transferToPlayerInventory(
                    player,
                    key.copyStackWithCount(request)
            );
            remaining = remaining - request + leftover.getCount();
            if (!leftover.isEmpty()) break;
        }
        return remaining;
    }
}
