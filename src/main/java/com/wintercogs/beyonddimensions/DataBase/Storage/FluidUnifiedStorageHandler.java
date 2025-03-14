package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class FluidUnifiedStorageHandler implements IFluidHandler
{

    private DimensionsNet net;

    public FluidUnifiedStorageHandler(DimensionsNet net) {
        this.net = net;
    }

    public void onChange()
    {
        net.setDirty();
    }

    public ArrayList<IStackType> getStorage()
    {
        return this.net.getUnifiedStorage().getStorage();
    }

    @Override
    public int getTanks()
    {
        int size = 0;
        List<IStackType> storage = getStorage();
        for (int i = 0; i < storage.size(); i++) {
            IStackType stackType = storage.get(i);
            if (stackType instanceof FluidStackType) {
                size++;
            }
        }
        return size;
    }

    @Override
    public FluidStack getFluidInTank(int slot)
    {
        int currentIndex = 0;
        List<IStackType> storage = getStorage();
        for (int i = 0; i < storage.size(); i++) {
            IStackType stackType = storage.get(i);
            if (stackType instanceof FluidStackType) {
                if(slot==currentIndex)
                {
                    return ((FluidStackType) stackType).copyStack();
                }
                else
                {
                    currentIndex++;
                }
            }
        }
        return FluidStack.EMPTY;
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
        int remaining = (int) net.getUnifiedStorage().insert(new FluidStackType(fluidStack.copy()), fluidAction.simulate()).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    // 返回实际导出数量
    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        return ((FluidStackType)net.getUnifiedStorage().extract(new FluidStackType(fluidStack.copy()),fluidAction.simulate()))
                .copyStack();
    }

    // 按数量导出流体
    // 此处处理为，尝试按数量导出第一个槽位的流体
    // 返回实际导出数量
    @Override
    public FluidStack drain(int count, FluidAction fluidAction)
    {
        return ((FluidStackType)net.getUnifiedStorage().extract(new FluidStackType(getFluidInTank(0).copyWithAmount(count)),fluidAction.simulate()))
                .copyStack();
    }
}
