package com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.InfusionStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraft.resources.ResourceLocation;

public class InfusionHandlerWrapper implements IStackHandlerWrapper<InfusionStack>
{
    private final IInfusionHandler chemicalHandler;

    public InfusionHandlerWrapper(Object chemicalHandler)
    {
        this.chemicalHandler = (IInfusionHandler) chemicalHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return InfusionStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return chemicalHandler.getTanks();
    }

    @Override
    public InfusionStack getStackInSlot(int slot)
    {
        return chemicalHandler.getChemicalInTank(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return chemicalHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, InfusionStack stack)
    {
        return chemicalHandler.isValid(slot, stack);
    }

    @Override
    public long insert(int slot, InfusionStack Stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.insertChemical(slot, Stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(slot, Stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long insert(InfusionStack stack, boolean sim)
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
    public long extract(InfusionStack stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.extractChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(stack, Action.EXECUTE).getAmount();
    }
}
