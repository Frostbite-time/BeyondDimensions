package com.wintercogs.beyonddimensions.Common.Processor.Capability;

import com.wintercogs.beyonddimensions.Common.InterfaceHelper.BlockCapabilityFunction;
import com.wintercogs.beyonddimensions.Common.InterfaceHelper.InsertAction;
import com.wintercogs.beyonddimensions.Common.InterfaceHelper.SlotsCount;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class SimpleBlockCapabilityProcessor<T> implements IBlockCapabilityProcessor<T>
{
    // 能力处理类
    private final BlockCapability<T,Direction> capabilityHandler;

    // 判断堆叠是否属于处理器类型
    private final Predicate<IStackType> stackChecker;

    // 用于从世界位置获取能力处理器的实例
    private final BlockCapabilityFunction<T> capabilityGetter;

    // 具体的插入逻辑实现
    private final InsertAction<T> insertAction;

    // 获取容器槽位数量
    private final SlotsCount<T> slotsCounter;

    public SimpleBlockCapabilityProcessor(
            BlockCapability<T,Direction> capabilityHandler,
            Predicate<IStackType> stackChecker,
            BlockCapabilityFunction<T> capabilityGetter,
            InsertAction<T> insertAction,
            SlotsCount<T> slotsCounter
    )
    {
        this.capabilityHandler = capabilityHandler;
        this.stackChecker = stackChecker;
        this.capabilityGetter = capabilityGetter;
        this.insertAction = insertAction;
        this.slotsCounter = slotsCounter;
    }

    @Override
    public boolean isStackType(IStackType wrapper)
    {
        return stackChecker.test(wrapper);
    }

    @Override
    public @Nullable T getCapability(Level level, BlockPos targetPos, Direction direction)
    {
        return capabilityGetter.get(level, targetPos, direction);
    }

    @Override
    public long insert(T handler, int slot, Object stack, boolean simulate)
    {
        return insertAction.insert(handler, slot, stack, simulate);
    }

    @Override
    public int getSlots(T handler)
    {
        return slotsCounter.getSlots(handler);
    }
}
