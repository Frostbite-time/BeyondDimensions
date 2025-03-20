package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(FluidStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public FluidStack getFluidInTank(int tank)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(FluidStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=tank && tank < slots.size())
        {
            actualIndex = slots.get(tank);
        }

        if(actualIndex != -1)
        {
            return (FluidStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return FluidStack.EMPTY;
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
        int actualIndex = handlerStorage.getTypeIdIndexList(FluidStackType.ID).getFirst();
        return ((FluidStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),fluidAction.simulate()))
                .copyStack();
    }
}
