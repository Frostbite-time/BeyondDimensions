package com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FluidHandlerWrapper implements IStackHandlerWrapper<FluidStack>
{
    private final ResourceHandler<@NotNull FluidResource> fluidHandler;
    private final Object rawHandler;

    public FluidHandlerWrapper(Object fluidHandler)
    {
        this.rawHandler = fluidHandler;
        this.fluidHandler = (ResourceHandler<FluidResource>) fluidHandler;
    }


    @Override
    public Identifier getTypeId()
    {
        return FluidStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return Math.max(0, fluidHandler.size());
    }

    @Override
    public FluidStack getStackInSlot(int slot)
    {
        if (slot < 0 || slot >= getSlots()) return FluidStack.EMPTY;

        FluidResource resource = fluidHandler.getResource(slot);
        int amount = fluidHandler.getAmountAsInt(slot);
        return resource.toStack(amount);
    }

    @Override
    public long getCapacity(int slot)
    {
        if (slot < 0 || slot >= getSlots()) return 0L;
        return Math.max(0L, fluidHandler.getCapacityAsLong(slot, fluidHandler.getResource(slot)));
    }

    @Override
    public boolean isStackValid(int slot, FluidStack stack)
    {
        if (slot < 0 || slot >= getSlots() || stack == null || stack.isEmpty()) return false;
        return fluidHandler.isValid(slot, FluidResource.of(stack));
    }

    @Override
    public long insert(int slot, FluidStack stack, boolean sim)
    {
        if (slot < 0 || slot >= getSlots() || stack == null || stack.isEmpty()) return 0L;

        int request = Math.max(0, stack.getAmount());
        if (request == 0) return 0L;

        try (Transaction tx = Transaction.openRoot())
        {
            int inserted = fluidHandler.insert(slot, FluidResource.of(stack), request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, (long) request - inserted);
        }
    }

    @Override
    public long insert(FluidStack stack, boolean sim)
    {
        if (stack == null || stack.isEmpty()) return 0L;

        int request = Math.max(0, stack.getAmount());
        if (request == 0) return 0L;

        try (Transaction tx = Transaction.openRoot())
        {
            int inserted = fluidHandler.insert(FluidResource.of(stack), request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, (long) request - inserted);
        }
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        if (slot < 0 || slot >= getSlots() || amount <= 0L) return 0L;

        FluidResource resource = fluidHandler.getResource(slot);
        if (resource.isEmpty()) return 0L;

        int request = (int) Math.min(amount, Integer.MAX_VALUE);
        try (Transaction tx = Transaction.openRoot())
        {
            int extracted = fluidHandler.extract(slot, resource, request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, extracted);
        }
    }

    @Override
    public long extract(FluidStack stack, boolean sim)
    {
        if (stack == null || stack.isEmpty()) return 0L;

        int request = Math.max(0, stack.getAmount());
        if (request == 0) return 0L;

        try (Transaction tx = Transaction.openRoot())
        {
            int extracted = fluidHandler.extract(FluidResource.of(stack), request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, extracted);
        }
    }

    @Override
    public Optional<ItemStack> getContainer()
    {
        try
        {
            var method = rawHandler.getClass().getMethod("getContainer");
            Object result = method.invoke(rawHandler);
            if (result instanceof ItemStack itemStack)
            {
                return Optional.of(itemStack);
            }
        }
        catch (ReflectiveOperationException ignored)
        {
        }
        return Optional.empty();
    }
}
