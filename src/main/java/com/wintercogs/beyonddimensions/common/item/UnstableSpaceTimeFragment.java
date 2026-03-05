package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class UnstableSpaceTimeFragment extends Item
{
    public UnstableSpaceTimeFragment(Properties properties)
    {
        super(properties.component(BDDataComponents.TIME_LINE, 0L));
    }


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);

        Long longData = stack.get(BDDataComponents.LONG_DATA);
        if (longData != null)
        {
            tooltipAdder.accept(Component.translatable(
                    "tooltip.item.unstable_space_time.long_data",
                    longData / 10
            ));
        }
        else
        {
            // 没初始化时显示默认值
            long def = ServerConfigRuntime.fragmentTransferTime;
            tooltipAdder.accept(Component.translatable(
                    "tooltip.item.unstable_space_time.long_data",
                    def / 10
            ));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot)
    {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof Player player))
        {
            return;
        }

        Long longDataObj = stack.get(BDDataComponents.LONG_DATA);
        if (longDataObj == null)
        {
            stack.set(BDDataComponents.LONG_DATA, ServerConfigRuntime.fragmentTransferTime);
            longDataObj = ServerConfigRuntime.fragmentTransferTime;
        }

        final long currentTick = level.getGameTime();
        final long lastProcessed = stack.getOrDefault(BDDataComponents.TIME_LINE, currentTick);

        // 每 200 tick（10 秒）处理一次，避免频繁同步
        if (currentTick - lastProcessed <= 200L)
        {
            return;
        }

        long longData = longDataObj;

        if (longData > 10L)
        {
            stack.set(BDDataComponents.LONG_DATA, longData - 10L);
            stack.set(BDDataComponents.TIME_LINE, currentTick);
            return;
        }

        // 剩余时间小于10直接转换
        ItemStack stable = new ItemStack(BDItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
        if (slot != null)
        {
            player.setItemSlot(slot, stable);
            return;
        }

        if (!replaceInInventoryByReference(player.getInventory(), stack, stable))
        {
            stack.set(BDDataComponents.TIME_LINE, currentTick);
        }
    }

    private static boolean replaceInInventoryByReference(Inventory inv, ItemStack target, ItemStack replacement)
    {
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (inv.getItem(i) == target)
            {
                inv.setItem(i, replacement);
                return true;
            }
        }
        return false;
    }
}
