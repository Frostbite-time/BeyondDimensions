package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public class FluidStackTypedHandler implements IFluidHandler
{

    private StackTypedHandler handlerStorage;

    public FluidStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }



    @Override
    public int getTanks()
    {
        return handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public FluidStack getFluidInTank(int tank)
    {
        return handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())  // 检查 tank 范围
                .map(slots -> slots.get(tank))                      // 提取 actualIndex
                .filter(actualIndex -> actualIndex >= 0)           // 过滤无效索引（如果实际索引可能为负）
                .map(handlerStorage::getStackBySlot)                // 获取存储对象（自动处理 null）
                .map(obj -> (FluidStack) obj.getStack())            // 直接转换，null 会被跳过
                .orElse(FluidStack.EMPTY);                          // 兜底返回空

    }

    @Override
    public int getTankCapacity(int tank)
    {
        return 64000;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluidStack)
    {
        return true;
    }

    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction)
    {
        if(fluidStack.isEmpty())
            return 0;
        int allAmount = fluidStack.getAmount();
        int remaining = (int) handlerStorage.insert(new FluidStackType(fluidStack.copy()), fluidAction.simulate()).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        return ((FluidStackType)handlerStorage.extract(new FluidStackType(fluidStack.copy()),fluidAction.simulate()))
                .copyStack();
    }

    @Override
    public FluidStack drain(int count, FluidAction fluidAction)
    {
        return handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                .map(slots -> slots.getFirst())                     // 提取第一个索引
                .filter(actualIndex -> actualIndex >= 0)            // 过滤无效索引
                .map(handlerStorage::getStackBySlot)                // 获取存储对象（自动处理 null）
                .map(stack -> stack.copyWithCount(count))                         // 复制对象（若 stack 为 null，此步自动跳过）
                .map(stack -> handlerStorage.extract(stack, fluidAction.simulate()))
                .map(extracts -> ((FluidStackType)extracts).copyStack())                     // 生成 FluidStack
                .orElse(FluidStack.EMPTY);                          // 兜底返回空
    }
}
