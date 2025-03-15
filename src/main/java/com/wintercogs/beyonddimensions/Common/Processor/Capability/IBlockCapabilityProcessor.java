package com.wintercogs.beyonddimensions.Common.Processor.Capability;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;


// 使用此处理器对各种stackHandler进行抽象
public interface IBlockCapabilityProcessor<T>
{
    // 用于判断当前处理器是否能处理对应堆
    boolean isStackType(IStackType wrapper);

    // 从对应位置获取对应方块的对应面的能力处理类
    @Nullable
    T getCapability(Level level, BlockPos targetPos, Direction direction);

    // 向目标插入一个堆叠，返回实际插入量
    long insert(T handler, int slot, Object stack, boolean simulate);

    // 返回总槽位数
    int getSlots(T handler);

    // 用于判断此处理器是否可用 如对某个mod的单向支持即可使用
    default boolean isAvailable() { return true; }

}
