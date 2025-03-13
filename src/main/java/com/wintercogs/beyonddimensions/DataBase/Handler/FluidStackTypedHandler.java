package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FluidStackTypedHandler implements IFluidHandler
{

    private StackTypedHandler handlerStorage;
    private List<Integer> fluidStorageIndex = new ArrayList<>(); //存储了ItemOnlyStorage的原Index对应，每次调用getItemOnlyStorage实时更新

    public FluidStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    // 获取所有可用于插入Item的槽位
    public List<FluidStackType> getFluidOnlyStorage()
    {
        fluidStorageIndex.clear();
        List<IStackType> storage = getStorage();

        // 第一次遍历：收集所有符合条件的索引 即空位置和符合类型的位置都可以用来插入当前类型
        for (int i = 0; i < storage.size(); i++) {
            IStackType stackType = storage.get(i);
            if (stackType.isEmpty() || stackType instanceof FluidStackType) {
                fluidStorageIndex.add(i);
            }
        }

        // 根据已知大小初始化ArrayList，避免扩容
        List<FluidStackType> result = new ArrayList<>(fluidStorageIndex.size());

        // 第二次遍历：填充结果列表
        for (int index : fluidStorageIndex) {
            IStackType stackType = storage.get(index);
            if (stackType.isEmpty()) {
                result.add(new FluidStackType());
            } else {
                result.add((FluidStackType) stackType);
            }
        }

        return result;
    }

    public List<IStackType> getStorage()
    {
        return this.handlerStorage.getStorage();
    }

    @Override
    public int getTanks()
    {
        return getFluidOnlyStorage().size();
    }

    @Override
    public FluidStack getFluidInTank(int tank)
    {
        return getFluidOnlyStorage().get(tank).copyStack();
    }

    @Override
    public int getTankCapacity(int tank)
    {
        return 64000;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluidStack)
    {
        return true;
    }

    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction)
    {
        if(fluidStack.isEmpty())
            return 0;
        int allAmount = fluidStack.getAmount();
        int remaining = (int) handlerStorage.insert(new FluidStackType(fluidStack.copy()), fluidAction.simulate()).getStackAmount();
        return allAmount-remaining;// 实际插入量
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        return ((FluidStackType)handlerStorage.extract(new FluidStackType(fluidStack.copy()),fluidAction.simulate()))
                .copyStack();
    }

    @Override
    public FluidStack drain(int count, FluidAction fluidAction)
    {
        return ((FluidStackType)handlerStorage.extract(new FluidStackType(getFluidOnlyStorage().getFirst().copyStackWithCount(count)),fluidAction.simulate()))
                .copyStack();
    }
}
