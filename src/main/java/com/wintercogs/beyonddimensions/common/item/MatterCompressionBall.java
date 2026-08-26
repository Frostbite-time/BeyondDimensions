package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand usedHand)
    {
        ItemStack ballStack = player.getItemInHand(usedHand);
        List<KeyAmount> contents = ballStack.getOrDefault(BDDataComponents.ISTACK_SLOTS, List.of());
        if (contents.isEmpty())
        {
            return InteractionResult.PASS;
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
                    ballStack.set(BDDataComponents.ISTACK_SLOTS, remainingContents);
                }
            }
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
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
