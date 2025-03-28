package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import mekanism.api.gas.*;
import net.minecraft.util.EnumFacing;


import javax.annotation.Nullable;
import java.util.List;

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
            // 此处的slot参数是基于特化类型ItemStackType的索引
            List<Integer> slots = handler.handlerStorage.getTypeIdIndexList(ChemicalStackType.ID);
            int actualIndex = -1;
            if(slots != null && 0<=tank && tank < slots.size())
            {
                actualIndex = slots.get(tank);
            }

            if(actualIndex != -1)
            {
                return (GasStack) handler.handlerStorage.getStackBySlot(actualIndex).getStack();
            }
            else return new GasStack(GasRegistry.getGas(0),0);
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID);
        if(slots != null)
        {
            GasTankInfo[] TankProperties = new GasTankInfo[slots.size()];

            for(int i = 0; i < slots.size(); i++)
            {
                TankProperties[i] = new GasTankInfoWarrper(i,this);
            }

            return TankProperties;
        }

        return new GasTankInfo[0];
    }


    // 返回插入量
    @Override
    public int receiveGas(EnumFacing enumFacing, GasStack stack, boolean sim)
    {
        if(stack.amount <=0)
            return 0;
        long remaining = handlerStorage.insert(new ChemicalStackType(stack.copy()), sim).getStackAmount();
        if(remaining>0)
            return (int) (stack.amount-remaining);
        return stack.amount;// 全部插入
    }

    @Override
    public GasStack drawGas(EnumFacing enumFacing, int amount, boolean sim)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(0);
        return ((ChemicalStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),sim))
                .copyStack();
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
