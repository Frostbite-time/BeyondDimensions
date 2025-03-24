package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

public class FluidUnifiedStorageHandler implements IFluidHandler
{

    private UnifiedStorage storage;

    public FluidUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        List<Integer> slots = storage.getTypeIdIndexList(FluidStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public FluidStack getFluidInTank(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = storage.getTypeIdIndexList(FluidStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (FluidStack) storage.getStackBySlot(actualIndex).getStack();
        }
        else return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int i)
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int slot, FluidStack fluidStack)
    {
        return true;
    }

    // 返回实际插入数量
    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction)
    {
        if(fluidStack.isEmpty())
            return 0;
        int allAmount = fluidStack.getAmount();
        int remaining = (int) storage.insert(new FluidStackType(fluidStack.copy()), fluidAction.simulate()).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    // 返回实际导出数量
    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        return ((FluidStackType)storage.extract(new FluidStackType(fluidStack.copy()),fluidAction.simulate()))
                .copyStack();
    }

    // 按数量导出流体
    // 此处处理为，尝试按数量导出第一个槽位的流体
    // 返回实际导出数量
    @Override
    public FluidStack drain(int count, FluidAction fluidAction)
    {
        return ((FluidStackType)storage.extract(new FluidStackType(new FluidStack(getFluidInTank(0),count)),fluidAction.simulate()))
                .copyStack();
    }
}
