package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.DataBase.LongType.EnergyType;
import com.wintercogs.beyonddimensions.DataBase.Stack.EnergyStackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyHandlerWrapper implements IStackHandlerWrapper<EnergyType>
{

    private final IEnergyStorage energyStorage;

    public EnergyHandlerWrapper(Object energyStorage)
    {
        this.energyStorage = (IEnergyStorage) energyStorage;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return EnergyStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public EnergyType getStackInSlot(int slot)
    {
        return new EnergyType(energyStorage.getEnergyStored());
    }

    @Override
    public long getCapacity(int slot)
    {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public boolean isStackValid(int slot, EnergyType stack)
    {
        return true;
    }

    @Override
    public long insert(int slot, EnergyType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        // 确保请求的插入量在int范围内（Max: 2,147,483,647）
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;

        // 接收能量并获取实际接受量
        int accepted = energyStorage.receiveEnergy(insertAmount, sim);

        // 计算未接收的余量 = 请求总量 - 实际接受量
        return amount - accepted;
    }

    @Override
    public long insert(EnergyType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        // 确保请求的插入量在int范围内（Max: 2,147,483,647）
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;

        // 接收能量并获取实际接受量
        int accepted = energyStorage.receiveEnergy(insertAmount, sim);

        // 计算未接收的余量 = 请求总量 - 实际接受量
        return amount - accepted;
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        int extractAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        if (extractAmount < 0) extractAmount = 0;
        return energyStorage.extractEnergy(extractAmount, sim);
    }

    @Override
    public long extract(EnergyType stack, boolean sim)
    {

        int extractAmount = (stack.getStackCount() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) stack.getStackCount();
        if (extractAmount < 0) extractAmount = 0;
        return energyStorage.extractEnergy(extractAmount, sim);
    }
}

