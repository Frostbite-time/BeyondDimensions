package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.item.Item;


import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class UnstableSpaceTimeFragment extends Item {

    // 1.12.2 构造器配置
    public UnstableSpaceTimeFragment() {
        this.setMaxStackSize(64); // 与原版属性保持一致
        this.setMaxDamage(0);     // 禁用耐久系统
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slotId, boolean isSelected) {
        super.onUpdate(stack, world, entity, slotId, isSelected);

        // 仅服务端处理 & 验证玩家实体
        if (!world.isRemote && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // 初始化NBT数据
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound tag = stack.getTagCompound();

            // 设置默认值
            if (!tag.hasKey("LongData")) {
                tag.setLong("LongData", 3600L);
            }
            if (!tag.hasKey("TimeLine")) {
                tag.setLong("TimeLine", 0L);
            }

            // 时间计算
            final long currentTick = world.getTotalWorldTime();
            final long lastProcessed = tag.getLong("TimeLine");

            // 200 tick (10秒) 间隔检测
            if (currentTick - lastProcessed > 200L) {
                long currentValue = tag.getLong("LongData");

                // 减少剩余时间
                if (currentValue > 0) {
                    tag.setLong("LongData", currentValue - 10);
                } else {
                    // 替换为稳定形态（保持堆叠数量）
                    ItemStack stable = new ItemStack(
                            ModItems.STABLE_SPACE_TIME_FRAGMENT,
                            stack.getCount()
                    );
                    player.inventory.setInventorySlotContents(slotId, stable);
                    return; // 终止后续操作
                }

                // 更新时间标记
                tag.setLong("TimeLine", currentTick);
                stack.setTagCompound(tag); // 必须重新设置NBT
            }
        }
    }

    // 保持原版辅助方法
    public static long getRemainingTime(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("LongData")) {
            return stack.getTagCompound().getLong("LongData");
        }
        return 3600L; // 与原版相同的默认值
    }
}

