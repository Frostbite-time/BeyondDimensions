package com.wintercogs.beyonddimensions.integration.module.ars.storage;

import com.hollingsworth.arsnouveau.api.source.ISourceCap;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import net.minecraft.resources.ResourceLocation;

public class SourceHandlerWrapper implements IStackHandlerWrapper<SourceType>
{
    private final ISourceCap sourceCap;

    public SourceHandlerWrapper(Object sourceCap)
    {
        this.sourceCap = (ISourceCap) sourceCap;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return SourceStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public SourceType getStackInSlot(int slot)
    {
        return new SourceType(sourceCap.getSource());
    }

    @Override
    public long getCapacity(int slot)
    {
        return sourceCap.getSourceCapacity();
    }

    @Override
    public boolean isStackValid(int slot, SourceType stack)
    {
        return true;
    }

    @Override
    public long insert(int slot, SourceType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        int accepted = sourceCap.receiveSource(insertAmount, sim);
        return amount - accepted;
    }

    @Override
    public long insert(SourceType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        int accepted = sourceCap.receiveSource(insertAmount, sim);
        return amount - accepted;
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        int extractAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        if (extractAmount < 0) extractAmount = 0;
        return sourceCap.extractSource(extractAmount, sim);
    }

    @Override
    public long extract(SourceType stack, boolean sim)
    {

        int extractAmount = (stack.getStackCount() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) stack.getStackCount();
        if (extractAmount < 0) extractAmount = 0;
        return sourceCap.extractSource(extractAmount, sim);
    }
}
