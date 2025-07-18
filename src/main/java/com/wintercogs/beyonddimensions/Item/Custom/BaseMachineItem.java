package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Machine.BaseMachine;
import com.wintercogs.beyonddimensions.Machine.RedStoneControlMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class BaseMachineItem extends NetedItem implements BaseMachine
{
    public BaseMachineItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        checkComponents(stack);

        if (level.isClientSide()) return;

        // 同时确保getTicksPerWork为0时可以每tick触发
        if(getTicksPerWork() <= 0)
            working(stack, level, entity, slotId, isSelected);
        else if(level.getGameTime() % getTicksPerWork() == 0)
            working(stack, level, entity, slotId, isSelected);
    }

    public void checkComponents(ItemStack stack)
    {
        if(!stack.has(ModDataComponents.CONTROL_MODE))
            stack.set(ModDataComponents.CONTROL_MODE, RedStoneControlMode.IGNORE);
    }

    @Override
    public void workStart(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        BaseMachine.super.workStart(stack, level, holder, slotId, isSelected);

    }

    @Override
    public RedStoneControlMode getControlMode()
    {
        return RedStoneControlMode.IGNORE;
    }

    @Override
    public RedStoneControlMode getControlMode(ItemStack stack)
    {
        return stack.has(ModDataComponents.CONTROL_MODE) ? stack.get(ModDataComponents.CONTROL_MODE) : RedStoneControlMode.IGNORE;
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
