package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class FluidStackTypedHandler implements IFluidHandler
{

    private DimensionsNet net;

    public FluidStackTypedHandler(DimensionsNet net) {
        this.net = net;
    }

    public ArrayList<FluidStackType> getFluidOnlyStorage()
    {
        return getStorage().stream()
                .filter(stackType -> stackType instanceof FluidStackType)
                .map(stackType -> (FluidStackType) stackType)  // 关键的类型转换
                .collect(Collectors.toCollection(ArrayList::new));
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
        return getFluidOnlyStorage().size();
    }

    @Override
    public FluidStack getFluidInTank(int slot)
    {
        return getFluidOnlyStorage().get(slot).getStack().copy();
    }

    @Override
    public int getTankCapacity(int i)
    {
        return Integer.MAX_VALUE-1;
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
        net.getUnifiedStorage().insert(new FluidStackType(fluidStack.copy()), fluidAction.simulate());
        return fluidStack.getAmount();// 始终全部插入
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
        return ((FluidStackType)net.getUnifiedStorage().extract(new FluidStackType(getFluidOnlyStorage().getFirst().copyStackWithCount(count)),fluidAction.simulate()))
                .copyStack();
    }
}
