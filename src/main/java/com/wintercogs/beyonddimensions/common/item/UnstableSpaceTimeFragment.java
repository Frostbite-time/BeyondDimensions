package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class UnstableSpaceTimeFragment extends Item
{
    public UnstableSpaceTimeFragment(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced)
    {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        tooltipComponents.add(Component.translatable("tooltip.item.unstable_space_time.long_data", getRemainingTime(stack) / 10));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof Player player))
        {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("LongData")) tag.putLong("LongData", 3600L);
        if (!tag.contains("TimeLine")) tag.putLong("TimeLine", 0L);

        final long currentTick = level.getGameTime();
        final long lastProcessed = tag.getLong("TimeLine");
        if (currentTick - lastProcessed <= 200L)
        {
            return;
        }

        long currentValue = tag.getLong("LongData");

        if (currentValue > 10)
        {
            tag.putLong("LongData", currentValue - 10);
            tag.putLong("TimeLine", currentTick);
            return;
        }

        // currentValue <= 10：直接转化
        int globalSlot = findGlobalSlotByReference(player.getInventory(), stack);
        if (globalSlot < 0)
        {
            tag.putLong("TimeLine", currentTick);
            return;
        }

        ItemStack stable = new ItemStack(BDItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
        player.getInventory().setItem(globalSlot, stable);
    }

    private static int findGlobalSlotByReference(Inventory inv, ItemStack target)
    {
        // items: 0..35
        for (int i = 0; i < inv.items.size(); i++)
        {
            if (inv.items.get(i) == target) return i;
        }
        // armor: 36..39
        for (int i = 0; i < inv.armor.size(); i++)
        {
            if (inv.armor.get(i) == target) return 36 + i;
        }
        // offhand: 40
        for (int i = 0; i < inv.offhand.size(); i++)
        {
            if (inv.offhand.get(i) == target) return 36 + 4 + i;
        }
        return -1;
    }

    // 辅助方法获取剩余时间
    public static long getRemainingTime(ItemStack stack)
    {
        if (stack.hasTag() && stack.getTag().contains("LongData"))
        {
            return stack.getTag().getLong("LongData");
        }
        return 3600L; // 默认值
    }
}
