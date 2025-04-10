package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import mekanism.api.gas.*;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ChemicalUnifiedStorageHandler implements IGasHandler
{

    private UnifiedStorage storage;

    public ChemicalUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    public static class GasTankInfoWarrper implements GasTankInfo
    {
        int tank;
        ChemicalUnifiedStorageHandler handler;

        public GasTankInfoWarrper(int tank, ChemicalUnifiedStorageHandler handler)
        {
            this.tank = tank;
            this.handler = handler;
        }


        @Nullable
        @Override
        public GasStack getGas()
        {
            // 此处的slot参数是基于特化类型ItemStackType的索引
            return handler.storage.getTypeIdIndexList(ChemicalStackType.ID)
                    .filter(slots -> tank>=0 && tank<slots.size())
                    .map(slots -> slots.get(tank))
                    .filter(actualIndex -> actualIndex>=0)
                    .map(actualIndex -> (ChemicalStackType)handler.storage.getStackBySlot(actualIndex))
                    .map(ChemicalStackType::getStack)
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
            return Integer.MAX_VALUE;
        }
    }


    public GasTankInfo[] getTankInfo()
    {
        return storage.getTypeIdIndexList(ChemicalStackType.ID)
                .map(slots -> {
                    GasTankInfo[] tankProperties = new GasTankInfo[slots.size()];
                    for (int i = 0; i < slots.size(); i++) {
                        tankProperties[i] = new ChemicalUnifiedStorageHandler.GasTankInfoWarrper(i, this);
                    }
                    return tankProperties;
                })
                .orElse(new GasTankInfo[0]);
    }


    @Override
    public int receiveGas(EnumFacing enumFacing, GasStack stack, boolean doAction)
    {
        boolean sim = !doAction;
        if(stack.amount <= 0)
            return 0;
        long remaining = storage.insert(new ChemicalStackType(stack.copy()), sim).getStackAmount();
        if(remaining>0)
            return (int) (stack.amount-remaining);
        return 0;// 全部插入
    }

    @Override
    public GasStack drawGas(EnumFacing enumFacing, int amount, boolean doAction)
    {
        boolean sim = !doAction;
        return storage.getTypeIdIndexList(ChemicalStackType.ID)
                // 获取第一个有效槽位索引的Optional
                .flatMap(list -> list.stream().findFirst())
                // 获取槽位中的堆栈对象（自动处理null）
                .flatMap(actualIndex -> Optional.ofNullable(storage.getStackBySlot(actualIndex)))
                // 复制堆栈内容（假设copy()不会返回null）
                .map(stack -> stack.copyWithCount(amount))
                // 执行提取操作并类型转换（假设extract返回非null）
                .flatMap(copiedStack ->
                        Optional.ofNullable((ChemicalStackType) storage.extract(copiedStack, sim)))
                // 最终复制堆栈数据
                .map(ChemicalStackType::copyStack)
                // 无有效结果时返回null（可替换为.orElseThrow()）
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
