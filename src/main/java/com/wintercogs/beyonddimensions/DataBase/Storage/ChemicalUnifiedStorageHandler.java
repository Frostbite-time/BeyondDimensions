package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChemicalUnifiedStorageHandler implements IChemicalHandler
{

    private DimensionsNet net;

    public ChemicalUnifiedStorageHandler(DimensionsNet net) {
        this.net = net;
    }

    public ArrayList<ChemicalStackType> getChemicalOnlyStorage()
    {
        List<IStackType> storage = getStorage();
        // 预分配最大可能容量，避免扩容
        ArrayList<ChemicalStackType> result = new ArrayList<>(storage.size());

        for (IStackType stackType : storage) {
            if (stackType instanceof ChemicalStackType) {
                // 直接类型转换，无需中间操作
                result.add((ChemicalStackType) stackType);
            }
        }
        // 可选：释放未使用的内存（根据场景决定是否需要）
        result.trimToSize();
        return result;
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
    public int getChemicalTanks()
    {
        return getChemicalOnlyStorage().size();
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank)
    {
        return getChemicalOnlyStorage().get(tank).getStack().copy();
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if(stack.isEmpty())
            return ;
        net.getUnifiedStorage().insert(new ChemicalStackType(stack.copy()), false);
    }

    @Override
    public long getChemicalTankCapacity(int tank)
    {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack)
    {
        return true;
    }

    // 返回剩余量，与Fluid的返回插入量不同
    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action)
    {
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = net.getUnifiedStorage().insert(new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        return ((ChemicalStackType)net.getUnifiedStorage().extract(new ChemicalStackType(getChemicalOnlyStorage().get(tank).copyStackWithCount(amount)),action.simulate()))
                .copyStack();
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action)
    {
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = net.getUnifiedStorage().insert(new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public ChemicalStack extractChemical(long amount, Action action)
    {
        return ((ChemicalStackType)net.getUnifiedStorage().extract(new ChemicalStackType(getChemicalOnlyStorage().getFirst().copyStackWithCount(amount)),action.simulate()))
                .copyStack();
    }

    // 按类型提取化学品
    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action)
    {
        return ((ChemicalStackType)net.getUnifiedStorage().extract(new ChemicalStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
