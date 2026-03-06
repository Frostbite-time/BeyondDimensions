package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.common.init.ModDataComponents;
import com.wintercogs.beyonddimensions.common.init.ModItems;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UnstableSpaceTimeFragment extends Item
{
    public UnstableSpaceTimeFragment(Properties properties)
    {
        super(properties.component(ModDataComponents.TIME_LINE, 0L));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag)
    {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        Long longData = stack.get(ModDataComponents.LONG_DATA);
        if (longData != null)
        {
            tooltipComponents.add(Component.translatable(
                    "tooltip.item.unstable_space_time.long_data",
                    longData / 10
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

        Long longDataObj = stack.get(ModDataComponents.LONG_DATA);
        if (longDataObj == null)
        {
            stack.set(ModDataComponents.LONG_DATA, ServerConfigRuntime.fragmentTransferTime);
            longDataObj = ServerConfigRuntime.fragmentTransferTime;
        }

        final long currentTick = level.getGameTime();
        final long lastProcessed = stack.getOrDefault(ModDataComponents.TIME_LINE, currentTick);

        // 每 200 tick（10 秒）处理一次，避免频繁同步
        if (currentTick - lastProcessed <= 200L)
        {
            return;
        }

        long longData = longDataObj;

        if (longData > 10L)
        {
            stack.set(ModDataComponents.LONG_DATA, longData - 10L);
            stack.set(ModDataComponents.TIME_LINE, currentTick);
            return;
        }

        // 剩余时间小于10直接转换
        int globalSlot = findGlobalSlotByReference(player.getInventory(), stack);
        if (globalSlot < 0)
        {
            stack.set(ModDataComponents.TIME_LINE, currentTick);
            return;
        }

        ItemStack stable = new ItemStack(ModItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
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
            if (inv.offhand.get(i) == target) return 36 + 4 + i; // 40
        }
        return -1;
    }
}