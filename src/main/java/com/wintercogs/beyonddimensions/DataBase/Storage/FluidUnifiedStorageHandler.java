package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.Optional;

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
            return handler.storage.getTypeIdIndexList(FluidStackType.ID)
                    .filter(slots -> tank>=0 && tank<slots.size())
                    .map(slots -> slots.get(tank))
                    .filter(actualIndex -> actualIndex>=0)
                    .map(actualIndex -> (FluidStackType)handler.storage.getStackBySlot(actualIndex))
                    .map(FluidStackType::getStack)
                    .orElse(new FluidStackType().getStack());
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
        return storage.getTypeIdIndexList(FluidStackType.ID)
                .map(slots -> {
                    IFluidTankProperties[] tankProperties = new IFluidTankProperties[slots.size()];
                    for (int i = 0; i < slots.size() + 1; i++) {
                        tankProperties[i] = new FluidUnifiedStorageHandler.TankProperties(i, this);
                    }
                    return tankProperties;
                })
                .orElse(new IFluidTankProperties[]{new FluidUnifiedStorageHandler.TankProperties(0, this)});
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
        return storage.getTypeIdIndexList(FluidStackType.ID)
                // 获取第一个有效槽位索引的Optional
                .flatMap(list -> list.stream().findFirst())
                // 获取槽位中的堆栈对象（自动处理null）
                .flatMap(actualIndex -> Optional.ofNullable(storage.getStackBySlot(actualIndex)))
                // 复制堆栈内容（假设copy()不会返回null）
                .map(stack -> stack.copyWithCount(count))
                // 执行提取操作并类型转换（假设extract返回非null）
                .flatMap(copiedStack ->
                        Optional.ofNullable((FluidStackType) storage.extract(copiedStack, sim)))
                // 最终复制堆栈数据
                .map(FluidStackType::copyStack)
                // 无有效结果时返回null（可替换为.orElseThrow()）
                .orElse(new FluidStackType().getStack());
    }
}
