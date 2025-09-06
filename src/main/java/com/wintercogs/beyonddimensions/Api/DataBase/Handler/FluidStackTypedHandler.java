package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class FluidStackTypedHandler implements IFluidHandler
{

    private StackHandler handlerStorage;

    public FluidStackTypedHandler(StackHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }



    @Override
    public int getTanks()
    {
        return handlerStorage.getBucket(FluidStackKey.ID)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int slot)
    {
        return handlerStorage.getBucket(FluidStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(handlerStorage::getStackBySlot)
                .map(stack -> {
                    Object outStack = handlerStorage.getOutStackByKey(stack.key());
                    if(outStack instanceof FluidStack fluidStack)
                    {
                        if(!fluidStack.isEmpty())
                            fluidStack.setAmount(BDMath.clampLongToInt(stack.amount()));
                        return fluidStack;
                    }
                    return null;
                })
                .orElse(FluidStack.EMPTY);

    }

    @Override
    public int getTankCapacity(int tank)
    {
        return 64000;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack fluidStack)
    {
        return true;
    }

    @Override
    public int fill(FluidStack fluidStack, @NotNull FluidAction fluidAction)
    {
        if(fluidStack.isEmpty())
            return 0;
        int allAmount = fluidStack.getAmount();
        int remaining = (int) handlerStorage.insert(new FluidStackKey(fluidStack), fluidStack.getAmount(),fluidAction.simulate()).amount();
        return allAmount-remaining;// 实际插入量
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        if(handlerStorage.extract(new FluidStackKey(fluidStack), fluidStack.getAmount(),fluidAction.simulate()).toStack() instanceof FluidStack result)
            return result;
        else
            return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int count, FluidAction fluidAction)
    {
        return handlerStorage.getBucket(FluidStackKey.ID)
                .map(slots -> slots.get(0))                     // 提取第一个索引
                .filter(actualIndex -> actualIndex >= 0)            // 过滤无效索引
                .map(handlerStorage::getStackBySlot)                // 获取存储对象（自动处理 null）
                .map(stack -> {
                    if(handlerStorage.extract(stack.key(),count,fluidAction.simulate()).toStack() instanceof FluidStack result)
                        return result;
                    else
                        return FluidStack.EMPTY;
                })
                .orElse(FluidStack.EMPTY);

    }
}
