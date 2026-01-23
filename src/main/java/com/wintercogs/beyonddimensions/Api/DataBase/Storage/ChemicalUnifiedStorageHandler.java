package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import org.jetbrains.annotations.NotNull;

public class ChemicalUnifiedStorageHandler implements IChemicalHandler
{

    private final UnifiedStorage storage;

    public ChemicalUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getChemicalTanks()
    {
        return storage.getBucket(ChemicalStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull ChemicalStack getChemicalInTank(int slot)
    {
        return storage.getBucket(ChemicalStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if (outStack instanceof ChemicalStack chemicalStack)
                    {
                        if (!chemicalStack.isEmpty())
                            chemicalStack.setAmount(storage.getStackByKey(key).amount());
                        return chemicalStack;
                    }
                    return null;
                })
                .orElse(ChemicalStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if (stack.isEmpty())
            return;
        storage.insert(new ChemicalStackKey(stack), stack.getAmount(), false);
    }

    @Override
    public long getChemicalTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public boolean isValid(int tank, @NotNull ChemicalStack stack)
    {
        return true;
    }

    // 返回剩余量，与Fluid的返回插入量不同
    @Override
    public @NotNull ChemicalStack insertChemical(int tank, ChemicalStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = storage.insert(new ChemicalStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public @NotNull ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        if (storage.extract(new ChemicalStackKey(getChemicalInTank(tank)), amount, action.simulate(), false).toStack() instanceof ChemicalStack result)
            return result;
        else
            return ChemicalStack.EMPTY;
    }

    @Override
    public @NotNull ChemicalStack insertChemical(ChemicalStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = storage.insert(new ChemicalStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public @NotNull ChemicalStack extractChemical(long amount, Action action)
    {
        if (storage.extract(new ChemicalStackKey(getChemicalInTank(0)), amount, action.simulate(), false).toStack() instanceof ChemicalStack result)
            return result;
        else
            return ChemicalStack.EMPTY;
    }

    // 按类型提取化学品
    @Override
    public @NotNull ChemicalStack extractChemical(@NotNull ChemicalStack stack, Action action)
    {
        if (storage.extract(new ChemicalStackKey(stack), stack.getAmount(), action.simulate(), false).toStack() instanceof ChemicalStack result)
            return result;
        else
            return ChemicalStack.EMPTY;
    }
}
