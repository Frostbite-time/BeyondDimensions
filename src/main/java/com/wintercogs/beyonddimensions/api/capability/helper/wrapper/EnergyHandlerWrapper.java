package com.wintercogs.beyonddimensions.api.capability.helper.wrapper;

import com.wintercogs.beyonddimensions.api.longtype.EnergyType;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class EnergyHandlerWrapper implements IStackHandlerWrapper<EnergyType>
{

    private final EnergyHandler energyStorage;

    public EnergyHandlerWrapper(Object energyStorage)
    {
        this.energyStorage = (EnergyHandler) energyStorage;
    }

    @Override
    public Identifier getTypeId()
    {
        return EnergyStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return 1;
    }

    @Override
    public EnergyType getStackInSlot(int slot)
    {
        if (slot != 0) return new EnergyType(0);
        return new EnergyType(energyStorage.getAmountAsLong());
    }

    @Override
    public long getCapacity(int slot)
    {
        if (slot != 0) return 0L;
        return energyStorage.getCapacityAsLong();
    }

    @Override
    public boolean isStackValid(int slot, EnergyType stack)
    {
        return true;
    }

    @Override
    public long insert(int slot, EnergyType stack, boolean sim)
    {
        if (slot != 0 || stack == null) return stack == null ? 0L : Math.max(0L, stack.getStackCount());

        long amount = Math.max(0L, stack.getStackCount());
        if (amount == 0L) return 0L;

        int insertAmount = (int) Math.min(amount, Integer.MAX_VALUE);
        try (Transaction tx = Transaction.openRoot())
        {
            int accepted = energyStorage.insert(insertAmount, tx);
            if (!sim) tx.commit();
            return Math.max(0L, amount - accepted);
        }
    }

    @Override
    public long insert(EnergyType stack, boolean sim)
    {
        if (stack == null) return 0L;

        long amount = Math.max(0L, stack.getStackCount());
        if (amount == 0L) return 0L;

        int insertAmount = (int) Math.min(amount, Integer.MAX_VALUE);
        try (Transaction tx = Transaction.openRoot())
        {
            int accepted = energyStorage.insert(insertAmount, tx);
            if (!sim) tx.commit();
            return Math.max(0L, amount - accepted);
        }
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        if (slot != 0 || amount <= 0L) return 0L;

        int extractAmount = (amount > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) amount;
        if (extractAmount < 0) extractAmount = 0;
        try (Transaction tx = Transaction.openRoot())
        {
            int extracted = energyStorage.extract(extractAmount, tx);
            if (!sim) tx.commit();
            return Math.max(0L, extracted);
        }
    }

    @Override
    public long extract(EnergyType stack, boolean sim)
    {
        if (stack == null) return 0L;

        int extractAmount = (stack.getStackCount() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) stack.getStackCount();
        if (extractAmount < 0) extractAmount = 0;
        try (Transaction tx = Transaction.openRoot())
        {
            int extracted = energyStorage.extract(extractAmount, tx);
            if (!sim) tx.commit();
            return Math.max(0L, extracted);
        }
    }
}
