package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.ArrayList;
import java.util.List;

public class ChemicalUnifiedStorageHandler implements IChemicalHandler
{

    private DimensionsNet net;

    public ChemicalUnifiedStorageHandler(DimensionsNet net) {
        this.net = net;
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
        List<Integer> slots = net.getUnifiedStorage().getTypeIdIndexList(ChemicalStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public ChemicalStack getChemicalInTank(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = net.getUnifiedStorage().getTypeIdIndexList(ChemicalStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (ChemicalStack) net.getUnifiedStorage().getStorage().get(actualIndex).copyStack();
        }
        else return ChemicalStack.EMPTY;
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
        return ((ChemicalStackType)net.getUnifiedStorage().extract(new ChemicalStackType(getChemicalInTank(tank).copyWithAmount(amount)),action.simulate()))
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
        return ((ChemicalStackType)net.getUnifiedStorage().extract(new ChemicalStackType(getChemicalInTank(0).copyWithAmount(amount)),action.simulate()))
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
