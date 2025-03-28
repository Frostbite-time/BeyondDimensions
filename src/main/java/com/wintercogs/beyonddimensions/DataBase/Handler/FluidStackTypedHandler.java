package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.List;

public class FluidStackTypedHandler implements IFluidHandler
{

    private StackTypedHandler handlerStorage;

    public FluidStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    public static class TankProperties implements IFluidTankProperties
    {

        int tank;
        FluidStackTypedHandler handler;

        public TankProperties(int tank, FluidStackTypedHandler handler)
        {
            this.tank = tank;
            this.handler = handler;
        }

        @Nullable
        @Override
        public FluidStack getContents()
        {
            // 此处的slot参数是基于特化类型ItemStackType的索引
            List<Integer> slots = handler.handlerStorage.getTypeIdIndexList(FluidStackType.ID);
            int actualIndex = -1;
            if(slots != null && 0<=tank && tank < slots.size())
            {
                actualIndex = slots.get(tank);
            }

            if(actualIndex != -1)
            {
                return (FluidStack) handler.handlerStorage.getStackBySlot(actualIndex).getStack();
            }
            else return new FluidStack(FluidRegistry.WATER,0);
        }

        @Override
        public int getCapacity()
        {
            return 64000;
        }

        @Override
        public boolean canFill()
        {
            return true;
        }

        @Override
        public boolean canDrain()
        {
            return true;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluidStack)
        {
            return true;
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluidStack)
        {
            return true;
        }
    }

    @Override
    public IFluidTankProperties[] getTankProperties()
    {
        List<Integer> slots = handlerStorage.getTypeIdIndexList(FluidStackType.ID);
        if(slots != null)
        {
            IFluidTankProperties[] TankProperties = new IFluidTankProperties[slots.size()];

            for(int i = 0; i < slots.size(); i++)
            {
                TankProperties[i] = new TankProperties(i,this);
            }

            return TankProperties;
        }

        return new IFluidTankProperties[0];
    }


    @Override
    public int fill(FluidStack fluidStack, boolean sim)
    {
        if(fluidStack.amount <= 0)
            return 0;
        int allAmount = fluidStack.amount;
        int remaining = (int) handlerStorage.insert(new FluidStackType(fluidStack.copy()), sim).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean sim)
    {
        return ((FluidStackType)handlerStorage.extract(new FluidStackType(fluidStack.copy()),sim))
                .copyStack();
    }

    @Override
    public FluidStack drain(int count, boolean sim)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(FluidStackType.ID).get(0);
        return ((FluidStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),sim))
                .copyStack();
    }


}
