package com.wintercogs.beyonddimensions.api.capability.helper.wrapper;

import com.wintercogs.beyonddimensions.api.storage.key.impl.GasStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.resources.ResourceLocation;

public class GasHandlerWrapper implements IStackHandlerWrapper<GasStack>
{
    private final IGasHandler chemicalHandler;

    public GasHandlerWrapper(Object chemicalHandler)
    {
        this.chemicalHandler = (IGasHandler) chemicalHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return GasStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return chemicalHandler.getTanks();
    }

    @Override
    public GasStack getStackInSlot(int slot)
    {
        return chemicalHandler.getChemicalInTank(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return chemicalHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, GasStack stack)
    {
        return chemicalHandler.isValid(slot, stack);
    }

    @Override
    public long insert(int slot, GasStack Stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.insertChemical(slot, Stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(slot, Stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long insert(GasStack stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.insertChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        if (sim)
            return chemicalHandler.extractChemical(slot, amount, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(slot, amount, Action.EXECUTE).getAmount();
    }

    @Override
    public long extract(GasStack stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.extractChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(stack, Action.EXECUTE).getAmount();
    }
}
