package com.wintercogs.beyonddimensions.integration.module.ifs.storage;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import net.minecraft.resources.ResourceLocation;

public class WardenSoulHandlerWrapper implements IStackHandlerWrapper<WardenSoulType>
{
    private final ISoulHandler soulHandler;

    public WardenSoulHandlerWrapper(Object soulHandler)
    {
        this.soulHandler = (ISoulHandler) soulHandler;
    }


    @Override
    public ResourceLocation getTypeId()
    {
        return WardenSoulStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return soulHandler.getSoulTanks();
    }

    @Override
    public WardenSoulType getStackInSlot(int slot)
    {
        return new WardenSoulType(soulHandler.getSoulInTank(slot));
    }

    @Override
    public long getCapacity(int slot)
    {
        return soulHandler.getTankCapacity(slot);
    }

    @Override
    public boolean isStackValid(int slot, WardenSoulType stack)
    {
        return true;
    }

    @Override
    public long insert(int slot, WardenSoulType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;
        int accepted = soulHandler.fill(insertAmount, action);
        return amount - accepted;
    }

    @Override
    public long insert(WardenSoulType stack, boolean sim)
    {
        long amount = stack.getStackCount();
        int insertAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;
        int accepted = soulHandler.fill(insertAmount, action);
        return amount - accepted;
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        int extractAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        if (extractAmount < 0) extractAmount = 0;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;
        return soulHandler.drain(extractAmount, action);
    }

    @Override
    public long extract(WardenSoulType stack, boolean sim)
    {
        int extractAmount = (stack.getStackCount() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) stack.getStackCount();
        if (extractAmount < 0) extractAmount = 0;
        ISoulHandler.Action action = sim ? ISoulHandler.Action.SIMULATE : ISoulHandler.Action.EXECUTE;
        return soulHandler.drain(extractAmount, action);
    }
}
