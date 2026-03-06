package com.wintercogs.beyonddimensions.common.machine;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// 物品机器的重写是可选的
// 方块机器相关的必须重写
public interface BaseMachine extends IMachine
{
    // working应当每tick调用一次
    @Override
    default void working()
    {
        if (shouldWork())
        {
            workStart();
            workContent();
            workEnd();
        }
    }

    @Override
    default void working(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        if (shouldWork(stack, level, holder, slotId, isSelected))
        {
            workStart(stack, level, holder, slotId, isSelected);
            workContent(stack, level, holder, slotId, isSelected);
            workEnd(stack, level, holder, slotId, isSelected);
        }
    }

    // 子类继承必须与父类用 与 运算，除非你完全确定不需要父类效果
    @Override
    default boolean shouldWork()
    {
        // 检测前增加步进时间，确保检测值为当前tick
        setStepTick(getStepTick() + 1); // 步进时间+1

        // 同时确保getTicksPerWork为0时可以每tick触发
        if (getStepTick() >= getTicksPerWork())
        {
            setStepTick(0); // 重置步进时间
            RedStoneControlMode controlMode = getControlMode();
            if (controlMode == null)
                return true;
            switch (controlMode)
            {
                case IGNORE ->
                {
                    return true;
                }
                case NOT_WORKING ->
                {
                    return false;
                }
                case POWERED ->
                {
                    return hasRedStoneSignal();
                }
                case UNPOWERED ->
                {
                    return !hasRedStoneSignal();
                }
            }
            return false;
        }
        return false;
    }

    // 物品的shouldWork需要自行处理步进时间
    @Override
    default boolean shouldWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        RedStoneControlMode controlMode = getControlMode(stack);
        if (controlMode == null) // 防止初始状态出错
            return true;
        switch (controlMode)
        {
            case IGNORE ->
            {
                return true;
            }
            case NOT_WORKING ->
            {
                return false;
            }
            case POWERED ->
            {
                return false;
            }
            case UNPOWERED ->
            {
                return true;
            }
        }
        return false;
    }

    RedStoneControlMode getControlMode();

    default RedStoneControlMode getControlMode(ItemStack stack)
    {
        return RedStoneControlMode.IGNORE;
    }

    boolean hasRedStoneSignal();

    default int getTicksPerWork()
    {
        return 0;
    }

    default int getTicksPerWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        return 0;
    }

    int getStepTick();

    void setStepTick(int newTick);

    default void workStart()
    {
    }

    default void workContent()
    {
    }

    default void workEnd()
    {
    }

    // workStart应该始终检查
    default void workStart(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
    }

    default void workContent(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
    }

    default void workEnd(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
    }
}
