package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidHandlerWrapper implements IStackHandlerWrapper<FluidStack>
{
    private final IFluidHandler fluidHandler;

    public FluidHandlerWrapper(Object fluidHandler)
    {
        this.fluidHandler = (IFluidHandler) fluidHandler;
    }


    @Override
    public ResourceLocation getTypeId()
    {
        return FluidStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return fluidHandler.getTanks();
    }

    @Override
    public FluidStack getStackInSlot(int slot)
    {
        return fluidHandler.getFluidInTank(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return fluidHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, FluidStack stack)
    {
        return fluidHandler.isFluidValid(slot, stack);
    }

    @Override
    public long insert(int slot, FluidStack stack, boolean sim)
    {
        // neoforge对流体没有按槽位插入的方案
        // 故直接调用无槽位方案
        return insert(stack, sim);
    }

    @Override
    public long insert(FluidStack stack, boolean sim)
    {
        int currentNum = stack.getAmount();
        int insert;
        if(sim)
            insert = fluidHandler.fill(stack, IFluidHandler.FluidAction.SIMULATE);
        else
            insert = fluidHandler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        return stack.copyWithAmount(currentNum-insert).getAmount();
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        for(int i = 0; i < getSlots(); i++)
        {
            FluidStack stack = getStackInSlot(i);
            if(!stack.isEmpty())
            {
                if(sim)
                    return fluidHandler.drain(stack.copyWithAmount((int)Math.min(amount,Integer.MAX_VALUE) ), IFluidHandler.FluidAction.SIMULATE).getAmount();
                else
                    return fluidHandler.drain(stack.copyWithAmount((int)Math.min(amount,Integer.MAX_VALUE) ), IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        }
        return 0;
    }

    @Override
    public long extract(FluidStack stack, boolean sim)
    {
        if(sim)
            return fluidHandler.drain(stack, IFluidHandler.FluidAction.SIMULATE).getAmount();
        else
            return fluidHandler.drain(stack, IFluidHandler.FluidAction.EXECUTE).getAmount();
    }
}
