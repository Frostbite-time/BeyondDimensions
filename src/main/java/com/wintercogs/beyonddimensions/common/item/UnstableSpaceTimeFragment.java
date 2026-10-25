package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UnstableSpaceTimeFragment extends Item
{
    // 物品 NBT 中的剩余时间与上次处理时的游戏时间
    private static final String LONG_DATA = "LongData";
    private static final String TIME_LINE = "TimeLine";

    public UnstableSpaceTimeFragment(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag)
    {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(LONG_DATA))
        {
            tooltipComponents.add(Component.translatable(
                    "tooltip.item.unstable_space_time.long_data",
                    tag.getLong(LONG_DATA) / 10
            ));
        }
        else
        {
            // 没初始化时显示默认值
            long def = ServerConfigRuntime.fragmentTransferTime;
            tooltipComponents.add(Component.translatable(
                    "tooltip.item.unstable_space_time.long_data",
                    def / 10
            ));
        }
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
        if (!tag.contains(LONG_DATA))
            tag.putLong(LONG_DATA, ServerConfigRuntime.fragmentTransferTime);

        final long currentTick = level.getGameTime();
        final long lastProcessed = tag.getLong(TIME_LINE);

        // 每 200 tick（10 秒）处理一次，避免频繁同步
        if (currentTick - lastProcessed <= 200L)
        {
            return;
        }

        long longData = tag.getLong(LONG_DATA);

        if (longData > 10L)
        {
            tag.putLong(LONG_DATA, longData - 10L);
            tag.putLong(TIME_LINE, currentTick);
            return;
        }

        // 剩余时间小于10直接转换
        int globalSlot = findGlobalSlotByReference(player.getInventory(), stack);
        if (globalSlot < 0)
        {
            tag.putLong(TIME_LINE, currentTick);
            return;
        }

        ItemStack stable = new ItemStack(BDItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
        player.getInventory().setItem(globalSlot, stable);
    }

    private static int findGlobalSlotByReference(Inventory inv, ItemStack target)
    {
        // 物品栏：0..35
        for (int i = 0; i < inv.items.size(); i++)
        {
            if (inv.items.get(i) == target) return i;
        }
        // 护甲：36..39
        for (int i = 0; i < inv.armor.size(); i++)
        {
            if (inv.armor.get(i) == target) return 36 + i;
        }
        // 副手：40
        for (int i = 0; i < inv.offhand.size(); i++)
        {
            if (inv.offhand.get(i) == target) return 36 + 4 + i; // 40
        }
        return -1;
    }
}