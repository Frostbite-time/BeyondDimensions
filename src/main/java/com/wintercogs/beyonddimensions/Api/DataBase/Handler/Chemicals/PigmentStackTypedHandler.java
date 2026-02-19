package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PigmentStackTypedHandler implements IPigmentHandler
{

    private static final ResourceLocation GAS_TYPE = PigmentStackKey.ID;

    private final StackHandler handlerStorage;

    public PigmentStackTypedHandler(StackHandler handlerStorage)
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
    public @NotNull PigmentStack getChemicalInTank(int slot)
    {
        if (!inGasRegion(slot)) return PigmentStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return PigmentStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return PigmentStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof PigmentStack gas)) return PigmentStack.EMPTY;
        if (gas.isEmpty()) return PigmentStack.EMPTY;

        long shown = ka.amount();
        if (shown <= 0) return PigmentStack.EMPTY;

        gas.setAmount(shown);
        return gas;
    }

    @Override
    public void setChemicalInTank(int tank, @NotNull PigmentStack stack)
    {
        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return;
        handlerStorage.setStackDirectly(actualIndex, new PigmentStackKey(stack), stack.getAmount());
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, @NotNull PigmentStack stack)
    {
        return true;
    }

    @Override
    public @NotNull PigmentStack insertChemical(int tank, @NotNull PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return PigmentStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return stack.copy();

        KeyAmount remaining = handlerStorage.insert(actualIndex, new PigmentStackKey(stack), stack.getAmount(), action.simulate());
        long rem = remaining.amount();
        return (rem > 0) ? new PigmentStack(stack, rem) : PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (amount <= 0) return PigmentStack.EMPTY;
        if (!inGasRegion(tank)) return PigmentStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return PigmentStack.EMPTY;

        Object out = handlerStorage.extract(actualIndex, amount, action.simulate()).toStack();
        return (out instanceof PigmentStack gs) ? gs : PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack insertChemical(@NotNull PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return PigmentStack.EMPTY;
        long remaining = handlerStorage.insert(new PigmentStackKey(stack), stack.getAmount(), action.simulate()).amount();
        return (remaining > 0) ? new PigmentStack(stack, remaining) : PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0) return PigmentStack.EMPTY;

        int firstGasSlot = handlerStorage.getBucket(GAS_TYPE)
                .map(b -> (b.size() > 0) ? b.get(0) : -1)
                .orElse(-1);
        if (firstGasSlot < 0) return PigmentStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(firstGasSlot);
        if (ka.isEmpty()) return PigmentStack.EMPTY;

        Object out = handlerStorage.extract(ka.key(), amount, action.simulate(), false).toStack();
        return (out instanceof PigmentStack gs) ? gs : PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack extractChemical(@NotNull PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return PigmentStack.EMPTY;
        Object out = handlerStorage.extract(new PigmentStackKey(stack), stack.getAmount(), action.simulate(), false).toStack();
        return (out instanceof PigmentStack gs) ? gs : PigmentStack.EMPTY;
    }
}
