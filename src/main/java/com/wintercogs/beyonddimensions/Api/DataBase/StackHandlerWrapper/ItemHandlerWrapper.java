package com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;

public class ItemHandlerWrapper implements IStackHandlerWrapper<ItemStack>
{
    private final ResourceHandler<@NotNull ItemResource> itemHandler;

    public ItemHandlerWrapper(Object itemHandler)
    {
        this.itemHandler = (ResourceHandler<ItemResource>) itemHandler;
    }

    @Override
    public Identifier getTypeId()
    {
        return ItemStackKey.ID;
    }

    @Override
    public int getSlots()
    {
        return Math.max(0, itemHandler.size());
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;

        ItemResource resource = itemHandler.getResource(slot);
        int amount = itemHandler.getAmountAsInt(slot);
        return resource.toStack(amount);
    }

    @Override
    public long getCapacity(int slot)
    {
        if (slot < 0 || slot >= getSlots()) return 0L;
        return Math.max(0L, itemHandler.getCapacityAsLong(slot, itemHandler.getResource(slot)));
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= getSlots() || stack == null || stack.isEmpty()) return false;
        return itemHandler.isValid(slot, ItemResource.of(stack));
    }

    @Override
    public long insert(int slot, ItemStack stack, boolean sim)
    {
        if (slot < 0 || slot >= getSlots() || stack == null || stack.isEmpty()) return 0L;

        int request = Math.max(0, stack.getCount());
        if (request == 0) return 0L;

        try (Transaction tx = Transaction.openRoot())
        {
            int inserted = itemHandler.insert(slot, ItemResource.of(stack), request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, (long) request - inserted);
        }
    }

    @Override
    public long insert(ItemStack stack, boolean sim)
    {
        if (stack == null || stack.isEmpty()) return 0L;

        int request = Math.max(0, stack.getCount());
        if (request == 0) return 0L;

        try (Transaction tx = Transaction.openRoot())
        {
            int inserted = itemHandler.insert(ItemResource.of(stack), request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, (long) request - inserted);
        }
    }

    @Override
    public long extract(int slot, long amount, boolean sim)
    {
        if (slot < 0 || slot >= getSlots() || amount <= 0L) return 0L;

        ItemResource resource = itemHandler.getResource(slot);
        if (resource.isEmpty()) return 0L;

        int request = (int) Math.min(amount, Integer.MAX_VALUE);
        try (Transaction tx = Transaction.openRoot())
        {
            int extracted = itemHandler.extract(slot, resource, request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, extracted);
        }
    }

    @Override
    public long extract(ItemStack stack, boolean sim)
    {
        if (stack == null || stack.isEmpty()) return 0L;

        int request = Math.max(0, stack.getCount());
        if (request == 0) return 0L;

        try (Transaction tx = Transaction.openRoot())
        {
            int extracted = itemHandler.extract(ItemResource.of(stack), request, tx);
            if (!sim) tx.commit();
            return Math.max(0L, extracted);
        }
    }
}
