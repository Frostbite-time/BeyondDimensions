package com.wintercogs.beyonddimensions.Machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public interface IMachine
{
    // 用于方块
    public void working();

    // 用于物品
    public void working(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot);

    // 用于方块
    public boolean shouldWork();

    // 用于物品
    public boolean shouldWork(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot);

}
