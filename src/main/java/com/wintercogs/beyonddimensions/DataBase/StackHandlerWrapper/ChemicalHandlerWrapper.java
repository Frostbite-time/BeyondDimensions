package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class ChemicalHandlerWrapper implements IStackHandlerWrapper<GasStack>
{
    private final IGasHandler chemicalHandler;

    public ChemicalHandlerWrapper(Object chemicalHandler)
    {
        this.chemicalHandler = (IGasHandler) chemicalHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return ChemicalStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return chemicalHandler.getTankInfo().length;
    }

    @Override
    public GasStack getStackInSlot(int slot)
    {
        return chemicalHandler.getTankInfo()[slot].getGas();
    }

    @Override
    public long getCapacity(int slot)
    {
        return chemicalHandler.getTankInfo()[slot].getMaxGas();
    }

    @Override
    public boolean isStackValid(EnumFacing facing,int slot, GasStack stack)
    {
        chemicalHandler.canReceiveGas(facing,stack.getGas());
        return true;
    }

    @Override
    public long insert(EnumFacing facing,int slot, GasStack Stack, boolean sim)
    {
        return chemicalHandler.receiveGas(facing,Stack, sim);
    }

    @Override
    public long insert(EnumFacing facing,GasStack stack, boolean sim)
    {
        return chemicalHandler.receiveGas(facing,stack, sim);
    }

    // 警告，不应调用此函数，因为此函数与实际接口不一致
    @Override
    public long extract(EnumFacing facing,int slot, long amount, boolean sim)
    {
        return chemicalHandler.drawGas(facing, (int) amount,sim).amount;
    }

    // 警告，不应调用此函数，因为此函数与实际接口不一致
    @Override
    public long extract(EnumFacing facing,GasStack stack, boolean sim)
    {
        return chemicalHandler.drawGas(facing, stack.amount,sim).amount;
    }
}
