package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChemicalStackTypedHandler implements IChemicalHandler
{

    private StackTypedHandler handlerStorage;
    private List<Integer> chemicalStorageIndex; //存储了ItemOnlyStorage的原Index对应，每次调用getItemOnlyStorage实时更新

    public ChemicalStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    // 获取所有可用于插入Item的槽位
    public List<ChemicalStackType> getChemicalOnlyStorage()
    {
        chemicalStorageIndex.clear(); // 清空索引列表
        return IntStream.range(0, getStorage().size())
                .mapToObj(i -> {
                    IStackType stackType = getStorage().get(i);
                    if (stackType.isEmpty() || stackType instanceof ChemicalStackType) {
                        chemicalStorageIndex.add(i); // 记录符合条件的索引
                        return stackType.isEmpty() ? new ChemicalStackType() : (ChemicalStackType) stackType;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<IStackType> getStorage()
    {
        return this.handlerStorage.getStorage();
    }

    @Override
    public int getChemicalTanks()
    {
        return getChemicalOnlyStorage().size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank)
    {
        return getChemicalOnlyStorage().get(tank).copyStack();
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack)
    {
        getChemicalOnlyStorage();
        getStorage().set(chemicalStorageIndex.get(tank), new ChemicalStackType(stack));
    }

    @Override
    public long getChemicalTankCapacity(int tank)
    {
        return 64000L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack)
    {
        return true;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action)
    {
        getChemicalOnlyStorage();
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(chemicalStorageIndex.get(tank),new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        getChemicalOnlyStorage();
        return ((ChemicalStackType)handlerStorage.extract(chemicalStorageIndex.get(tank),amount,action.simulate()))
                .copyStack();
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action)
    {
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;// 始终全部插入
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(new ChemicalStackType(getChemicalOnlyStorage().getFirst().copyStackWithCount(amount)),action.simulate()))
                .copyStack();
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(new ChemicalStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
