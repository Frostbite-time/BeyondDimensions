package com.wintercogs.beyonddimensions.api.capability.helper.ordered;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.InfusionStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class InfusionStackTypedHandler implements IInfusionHandler
{

    private static final ResourceLocation GAS_TYPE = InfusionStackKey.ID;

    private final StackHandler handlerStorage;

    public InfusionStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    private int gasCount()
    {
        return handlerStorage.getBucket(GAS_TYPE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    private int emptyCount()
    {
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    private boolean inGasRegion(int visibleSlot)
    {
        int gases = gasCount();
        return visibleSlot >= 0 && visibleSlot < gases;
    }

    private int getGasSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(GAS_TYPE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    private int getEmptySlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    private int resolveActualIndex(int visibleSlot)
    {
        if (visibleSlot < 0) return -1;
        int gases = gasCount();
        if (visibleSlot < gases)
        {
            return getGasSlotAt(visibleSlot);
        }
        int rest = visibleSlot - gases;
        return getEmptySlotAt(rest);
    }

    @Override
    public int getTanks()
    {
        return gasCount() + emptyCount();
    }

    @Override
    public @NotNull InfusionStack getChemicalInTank(int slot)
    {
        if (!inGasRegion(slot)) return InfusionStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return InfusionStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return InfusionStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof InfusionStack gas)) return InfusionStack.EMPTY;
        if (gas.isEmpty()) return InfusionStack.EMPTY;

        long shown = ka.amount();
        if (shown <= 0) return InfusionStack.EMPTY;

        gas.setAmount(shown);
        return gas;
    }

    @Override
    public void setChemicalInTank(int tank, @NotNull InfusionStack stack)
    {
        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return;
        handlerStorage.setStackDirectly(actualIndex, new InfusionStackKey(stack), stack.getAmount());
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, @NotNull InfusionStack stack)
    {
        return true;
    }

    @Override
    public @NotNull InfusionStack insertChemical(int tank, @NotNull InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return stack.copy();

        KeyAmount remaining = handlerStorage.insert(actualIndex, new InfusionStackKey(stack), stack.getAmount(), action.simulate());
        long rem = remaining.amount();
        return (rem > 0) ? new InfusionStack(stack, rem) : InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (amount <= 0) return InfusionStack.EMPTY;
        if (!inGasRegion(tank)) return InfusionStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return InfusionStack.EMPTY;

        Object out = handlerStorage.extract(actualIndex, amount, action.simulate()).toStack();
        return (out instanceof InfusionStack gs) ? gs : InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack insertChemical(@NotNull InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;
        long remaining = handlerStorage.insert(new InfusionStackKey(stack), stack.getAmount(), action.simulate()).amount();
        return (remaining > 0) ? new InfusionStack(stack, remaining) : InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0) return InfusionStack.EMPTY;

        int firstGasSlot = handlerStorage.getBucket(GAS_TYPE)
                .map(b -> (b.size() > 0) ? b.get(0) : -1)
                .orElse(-1);
        if (firstGasSlot < 0) return InfusionStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(firstGasSlot);
        if (ka.isEmpty()) return InfusionStack.EMPTY;

        Object out = handlerStorage.extract(ka.key(), amount, action.simulate(), false).toStack();
        return (out instanceof InfusionStack gs) ? gs : InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack extractChemical(@NotNull InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;
        Object out = handlerStorage.extract(new InfusionStackKey(stack), stack.getAmount(), action.simulate(), false).toStack();
        return (out instanceof InfusionStack gs) ? gs : InfusionStack.EMPTY;
    }
}
