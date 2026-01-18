package com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import net.minecraft.resources.ResourceLocation;

public class PigmentHandlerWrapper implements IStackHandlerWrapper<PigmentStack>
{
    private final IPigmentHandler chemicalHandler;

    public PigmentHandlerWrapper(Object chemicalHandler)
    {
        this.chemicalHandler = (IPigmentHandler) chemicalHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return PigmentStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return chemicalHandler.getTanks();
    }

    @Override
    public PigmentStack getStackInSlot(int slot)
    {
        return chemicalHandler.getChemicalInTank(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return chemicalHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, PigmentStack stack)
    {
        return chemicalHandler.isValid(slot, stack);
    }

    @Override
    public long insert(int slot, PigmentStack Stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.insertChemical(slot, Stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(slot, Stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long insert(PigmentStack stack, boolean sim)
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
    public long extract(PigmentStack stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.extractChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(stack, Action.EXECUTE).getAmount();
    }
}
