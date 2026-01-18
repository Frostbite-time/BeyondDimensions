package com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.resources.ResourceLocation;

public class SlurryHandlerWrapper implements IStackHandlerWrapper<SlurryStack>
{
    private final ISlurryHandler chemicalHandler;

    public SlurryHandlerWrapper(Object chemicalHandler)
    {
        this.chemicalHandler = (ISlurryHandler) chemicalHandler;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return SlurryStackType.ID;
    }

    @Override
    public int getSlots()
    {
        return chemicalHandler.getTanks();
    }

    @Override
    public SlurryStack getStackInSlot(int slot)
    {
        return chemicalHandler.getChemicalInTank(slot);
    }

    @Override
    public long getCapacity(int slot)
    {
        return chemicalHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, SlurryStack stack)
    {
        return chemicalHandler.isValid(slot, stack);
    }

    @Override
    public long insert(int slot, SlurryStack Stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.insertChemical(slot, Stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.insertChemical(slot, Stack, Action.EXECUTE).getAmount();
    }

    @Override
    public long insert(SlurryStack stack, boolean sim)
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
    public long extract(SlurryStack stack, boolean sim)
    {
        if (sim)
            return chemicalHandler.extractChemical(stack, Action.SIMULATE).getAmount();
        else
            return chemicalHandler.extractChemical(stack, Action.EXECUTE).getAmount();
    }
}
