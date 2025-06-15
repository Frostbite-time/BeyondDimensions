package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidUnifiedStorageHandler implements IFluidHandler
{

    private UnifiedStorage storage;

    public FluidUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getTypeIdIndexList(FluidStackType.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size()+1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public FluidStack getFluidInTank(int slot)
    {
        return storage.getTypeIdIndexList(FluidStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (FluidStackType)storage.getStackBySlot(actualIndex))
                .map(FluidStackType::getStack)
                .orElse(FluidStack.EMPTY);
    }

    @Override
    public int getTankCapacity(int tank)
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
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
        return ((FluidStackType)storage.extract(new FluidStackType(getFluidInTank(0).copyWithAmount(count)),fluidAction.simulate()))
                .copyStack();
    }
}
