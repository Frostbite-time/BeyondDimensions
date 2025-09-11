package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class FluidUnifiedStorageHandler implements IFluidHandler
{

    private final UnifiedStorage storage;

    public FluidUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getBucket(FluidStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size()+1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int slot)
    {
        return storage.getBucket(FluidStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if(outStack instanceof FluidStack fluidStack)
                    {
                        if(!fluidStack.isEmpty())
                            fluidStack.setAmount(BDMath.clampLongToInt(storage.getStackByKey(key).amount()));
                        return fluidStack;
                    }
                    return null;
                })
                .orElse(FluidStack.EMPTY);
    }

    @Override
    public int getTankCapacity(int tank)
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
    }

    @Override
    public boolean isFluidValid(int slot, @NotNull FluidStack fluidStack)
    {
        return true;
    }

    // 返回实际插入数量
    @Override
    public int fill(FluidStack fluidStack, @NotNull FluidAction fluidAction)
    {
        if(fluidStack.isEmpty())
            return 0;
        int allAmount = fluidStack.getAmount();
        int remaining = (int) storage.insert(new FluidStackKey(fluidStack), fluidStack.getAmount(), fluidAction.simulate()).amount();
        return allAmount-remaining;// 实际插入量
    }

    // 返回实际导出数量
    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack fluidStack, FluidAction fluidAction)
    {
        if(storage.extract(new FluidStackKey(fluidStack), fluidStack.getAmount(),fluidAction.simulate()).toStack() instanceof FluidStack result)
            return result;
        else
            return FluidStack.EMPTY;
    }

    // 按数量导出流体
    // 此处处理为，尝试按数量导出第一个槽位的流体
    // 返回实际导出数量
    @Override
    public @NotNull FluidStack drain(int count, FluidAction fluidAction)
    {
        if(storage.extract(new FluidStackKey(getFluidInTank(0)), count,fluidAction.simulate()).toStack() instanceof FluidStack result)
            return result;
        else
            return FluidStack.EMPTY;
    }
}
