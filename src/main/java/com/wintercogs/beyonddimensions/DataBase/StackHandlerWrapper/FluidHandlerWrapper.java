package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
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
        return fluidHandler.getTankProperties().length;
    }

    @Override
    public FluidStack getStackInSlot(int slot)
    {
        return fluidHandler.getTankProperties()[slot].getContents();
    }

    @Override
    public long getCapacity(int slot)
    {
        return fluidHandler.getTankProperties()[slot].getCapacity();
    }

    @Override
    public boolean isStackValid(EnumFacing facing,int slot, FluidStack stack)
    {
        return fluidHandler.getTankProperties()[slot].canFillFluidType(stack);
    }

    @Override
    public long insert(EnumFacing facing, int slot, FluidStack stack, boolean sim)
    {
        // neoforge对流体没有按槽位插入的方案
        // 故直接调用无槽位方案
        return insert(stack, sim);
    }

    @Override
    public long insert(EnumFacing facing,FluidStack stack, boolean sim)
    {
        int currentNum = stack.amount;
        int insert;
        insert = fluidHandler.fill(stack, sim);
        return currentNum-insert;
    }

    @Override
    public long extract(EnumFacing facing,int slot, long amount, boolean sim)
    {
        for(int i = 0; i < getSlots(); i++)
        {
            FluidStack stack = getStackInSlot(i);
            if(!(stack.amount <=0))
            {
                return fluidHandler.drain(new FluidStack(stack,(int)Math.min(amount,Integer.MAX_VALUE)), sim).amount;
            }
        }
        return 0;
    }

    @Override
    public long extract(EnumFacing facing,FluidStack stack, boolean sim)
    {
        return fluidHandler.drain(stack, sim).amount;
    }
}
