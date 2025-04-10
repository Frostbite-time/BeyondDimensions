package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class ChemicalStackTypedHandler implements IGasHandler
{

    private StackTypedHandler handlerStorage;

    public ChemicalStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    public static class GasTankInfoWarrper implements GasTankInfo
    {
        int tank;
        ChemicalStackTypedHandler handler;

        public GasTankInfoWarrper(int tank, ChemicalStackTypedHandler handler)
        {
            this.tank = tank;
            this.handler = handler;
        }


        @Nullable
        @Override
        public GasStack getGas()
        {
            return handler.handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                    .filter(slots -> tank >= 0 && tank < slots.size())
                    .map(slots -> slots.get(tank))
                    .filter(actualIndex -> actualIndex >= 0)
                    .map(handler.handlerStorage::getStackBySlot)
                    .map(obj -> (GasStack) obj.getStack())
                    .orElse(new ChemicalStackType().getStack());
        }

        @Override
        public int getStored()
        {
            return getGas().amount;
        }

        @Override
        public int getMaxGas()
        {
            return 64000;
        }
    }



    public GasTankInfo[] getTankInfo()
    {
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .map(slots -> {
                    GasTankInfo[] tankProperties = new GasTankInfo[slots.size()];
                    for (int i = 0; i < slots.size(); i++) {
                        tankProperties[i] = new ChemicalStackTypedHandler.GasTankInfoWarrper(i, this);
                    }
                    return tankProperties;
                })
                .orElse(new GasTankInfo[0]);
    }


    // 返回插入量
    @Override
    public int receiveGas(EnumFacing enumFacing, GasStack stack, boolean doAction)
    {
        boolean sim = !doAction;
        if(stack.amount <=0)
            return 0;
        long remaining = handlerStorage.insert(new ChemicalStackType(stack.copy()), sim).getStackAmount();
        if(remaining>0)
            return (int) (stack.amount-remaining);
        return stack.amount;// 全部插入
    }


    @Override
    public GasStack drawGas(EnumFacing enumFacing, int amount, boolean doAction)
    {
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copy())
                .map(stack -> handlerStorage.extract(stack, !doAction))
                .map(extracts -> ((ChemicalStackType)extracts).copyStack())
                .orElse(new ChemicalStackType().getStack());
    }

    @Override
    public boolean canReceiveGas(EnumFacing enumFacing, Gas gas)
    {
        return true;
    }

    @Override
    public boolean canDrawGas(EnumFacing enumFacing, Gas gas)
    {
        return true;
    }

}
