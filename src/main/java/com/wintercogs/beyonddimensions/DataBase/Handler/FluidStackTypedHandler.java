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
            return handler.handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                    .filter(slots -> tank >= 0 && tank < slots.size())  // 检查 tank 范围
                    .map(slots -> slots.get(tank))                      // 提取 actualIndex
                    .filter(actualIndex -> actualIndex >= 0)           // 过滤无效索引（如果实际索引可能为负）
                    .map(handler.handlerStorage::getStackBySlot)                // 获取存储对象（自动处理 null）
                    .map(obj -> (FluidStack) obj.getStack())            // 直接转换，null 会被跳过
                    .orElse(new FluidStackType().getStack());                          // 兜底返回空
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
        return handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                .map(slots -> {
                    IFluidTankProperties[] tankProperties = new IFluidTankProperties[slots.size()];
                    for (int i = 0; i < slots.size(); i++) {
                        tankProperties[i] = new TankProperties(i, this);
                    }
                    return tankProperties;
                })
                .orElse(new IFluidTankProperties[0]);
    }


    @Override
    public int fill(FluidStack fluidStack, boolean doAction)
    {
        boolean sim = !doAction;
        if(fluidStack.amount <= 0)
            return 0;
        int allAmount = fluidStack.amount;
        int remaining = (int) handlerStorage.insert(new FluidStackType(fluidStack.copy()), sim).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean doAction)
    {
        boolean sim = !doAction;
        return ((FluidStackType)handlerStorage.extract(new FluidStackType(fluidStack.copy()),sim))
                .copyStack();
    }

    @Override
    public FluidStack drain(int count, boolean doAction)
    {
        return handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                .map(slots -> slots.get(0))                     // 提取第一个索引
                .filter(actualIndex -> actualIndex >= 0)            // 过滤无效索引
                .map(handlerStorage::getStackBySlot)                // 获取存储对象（自动处理 null）
                .map(stack -> stack.copy())                         // 复制对象（若 stack 为 null，此步自动跳过）
                .map(stack -> handlerStorage.extract(stack, !doAction))
                .map(extracts -> ((FluidStackType)extracts).copyStack())                     // 生成 FluidStack
                .orElse(new FluidStackType().getStack());                          // 兜底返回空
    }


}
