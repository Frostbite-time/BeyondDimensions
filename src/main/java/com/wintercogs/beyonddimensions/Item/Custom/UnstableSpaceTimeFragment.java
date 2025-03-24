package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class UnstableSpaceTimeFragment extends Item
{
    public UnstableSpaceTimeFragment(Properties properties) {
        super(properties);
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide() && entity instanceof Player player) {
            CompoundTag tag = stack.getOrCreateTag();

            // 初始化默认值
            if (!tag.contains("LongData")) {
                tag.putLong("LongData", 3600L);
            }
            if (!tag.contains("TimeLine")) {
                tag.putLong("TimeLine", 0L);
            }
            final long currentTick = level.getGameTime();
            final long lastProcessed = tag.getLong("TimeLine");

            if (currentTick - lastProcessed > 200L) {
                long currentValue = tag.getLong("LongData");

                if (currentValue > 0) {
                    tag.putLong("LongData", currentValue - 10);
                } else {
                    // 替换物品并保持堆叠数量
                    ItemStack stable = new ItemStack(ModItems.STABLE_SPACE_TIME_FRAGMENT.get(), stack.getCount());
                    player.getInventory().setItem(slotId, stable);
                    return; // 提前返回避免修改已替换的物品
                }

                tag.putLong("TimeLine", currentTick);
            }
        }
    }
    // 辅助方法获取剩余时间
    public static long getRemainingTime(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("LongData")) {
            return stack.getTag().getLong("LongData");
        }
        return 3600L; // 默认值
    }
}
