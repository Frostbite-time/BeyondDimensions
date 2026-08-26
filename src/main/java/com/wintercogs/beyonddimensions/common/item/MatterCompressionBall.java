package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MatterCompressionBall extends Item
{
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
