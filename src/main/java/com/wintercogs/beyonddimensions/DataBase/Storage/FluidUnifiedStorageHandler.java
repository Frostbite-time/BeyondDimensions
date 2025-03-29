package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.List;

public class FluidUnifiedStorageHandler implements IFluidHandler
{

    private UnifiedStorage storage;

    public FluidUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    public static class TankProperties implements IFluidTankProperties
    {

        int tank;
        FluidUnifiedStorageHandler handler;

        public TankProperties(int tank, FluidUnifiedStorageHandler handler)
        {
            this.tank = tank;
            this.handler = handler;
        }

        @Nullable
        @Override
        public FluidStack getContents()
        {
            // 此处的slot参数是基于特化类型ItemStackType的索引
            List<Integer> slots = handler.storage.getTypeIdIndexList(FluidStackType.ID);
            int actualIndex = -1;
            if(slots != null && 0<=tank && tank < slots.size())
            {
                actualIndex = slots.get(tank);
            }

            if(actualIndex != -1)
            {
                return (FluidStack) handler.storage.getStackBySlot(actualIndex).getStack();
            }
            else return new FluidStack(FluidRegistry.WATER,0);
        }

        @Override
        public int getCapacity()
        {
            return Integer.MAX_VALUE;
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
        List<Integer> slots = storage.getTypeIdIndexList(FluidStackType.ID);
        if(slots != null)
        {
            IFluidTankProperties[] TankProperties = new IFluidTankProperties[slots.size()];

            for(int i = 0; i < slots.size(); i++)
            {
                TankProperties[i] = new FluidUnifiedStorageHandler.TankProperties(i,this);
            }

            return TankProperties;
        }

        return new IFluidTankProperties[0];
    }

    // 返回实际插入数量
    @Override
    public int fill(FluidStack fluidStack, boolean doAction)
    {
        boolean sim = !doAction;
        if(fluidStack.amount <=0)
            return 0;
        int allAmount = fluidStack.amount;
        int remaining = (int) storage.insert(new FluidStackType(fluidStack.copy()), sim).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    // 返回实际导出数量
    @Override
    public FluidStack drain(FluidStack fluidStack, boolean doAction)
    {
        boolean sim = !doAction;
        return ((FluidStackType)storage.extract(new FluidStackType(fluidStack.copy()),sim))
                .copyStack();
    }

    // 按数量导出流体
    // 此处处理为，尝试按数量导出第一个槽位的流体
    // 返回实际导出数量
    @Override
    public FluidStack drain(int count, boolean doAction)
    {
        boolean sim = !doAction;
        int actualIndex = storage.getTypeIdIndexList(FluidStackType.ID).get(0);
        return ((FluidStackType)storage.extract(storage.getStackBySlot(actualIndex).copy(),sim))
                .copyStack();
    }
}
