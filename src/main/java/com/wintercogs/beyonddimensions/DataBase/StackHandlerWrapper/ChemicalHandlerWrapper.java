package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.resources.ResourceLocation;

public class ChemicalHandlerWrapper implements IStackHandlerWrapper<ChemicalStack>
{
    private final IChemicalHandler chemicalHandler;

    public ChemicalHandlerWrapper(Object chemicalHandler)
    {
        this.chemicalHandler = (IChemicalHandler) chemicalHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return ChemicalStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return chemicalHandler.getTanks();
    }

    @Override
    public ChemicalStack getStackInSlot(int slot)
    {
        return chemicalHandler.getChemicalInTank(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return chemicalHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, ChemicalStack stack)
    {
        return chemicalHandler.isValid(slot, stack);
    }

    @Override
    public long insert(int slot, ChemicalStack Stack, boolean sim)
    {
        if(sim)
            return chemicalHandler.insertChemical(slot,Stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(slot,Stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long insert(ChemicalStack stack, boolean sim)
    {
        if(sim)
            return chemicalHandler.insertChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        if(sim)
            return chemicalHandler.extractChemical(slot, amount, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(slot, amount, Action.EXECUTE).getAmount();
    }

    @Override
    public long extract(ChemicalStack stack, boolean sim)
    {
        if(sim)
            return chemicalHandler.extractChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(stack, Action.EXECUTE).getAmount();
    }
}
