package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.machine.BaseMachine;
import com.wintercogs.beyonddimensions.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public abstract class BaseMachineItem extends NetedItem implements BaseMachine
{
    public BaseMachineItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot)
    {
        super.inventoryTick(stack, level, entity, slot);

        checkComponents(stack);

        if (level.isClientSide()) return;

        // 同时确保getTicksPerWork为0时可以每tick触发
        if (getTicksPerWork(stack, level, entity, slot) <= 0)
            working(stack, level, entity, slot);
        else if (level.getGameTime() % getTicksPerWork(stack, level, entity, slot) == 0)
            working(stack, level, entity, slot);
    }

    public void checkComponents(ItemStack stack)
    {
        if (!stack.has(BDDataComponents.CONTROL_MODE))
            stack.set(BDDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
    }

    @Override
    public void workStart(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot)
    {
        BaseMachine.super.workStart(stack, level, entity, slot);
    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return RedStoneControlMode.IGNORE;
    }

    @Override
    public RedStoneControlMode getControlMode(ItemStack stack)
    {
        return stack.has(BDDataComponents.CONTROL_MODE) ? stack.get(BDDataComponents.CONTROL_MODE) : RedStoneControlMode.IGNORE;
    }

    @Override
    public boolean hasRedStoneSignal()
    {
        return false;
    }

    @Override
    public int getStepTick()
    {
        return 0;
    }

    @Override
    public void setStepTick(int newTick)
    {

    }
}
