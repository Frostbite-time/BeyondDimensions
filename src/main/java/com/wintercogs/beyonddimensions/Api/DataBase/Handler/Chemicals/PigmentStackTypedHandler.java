package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import org.jetbrains.annotations.NotNull;

public class PigmentStackTypedHandler implements IPigmentHandler
{

    private final StackTypedHandler handlerStorage;

    public PigmentStackTypedHandler(StackTypedHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 将统一存储中的所有槽位视为潜在的 Pigment 槽位。
     */
    @Override
    public int getTanks()
    {
        return handlerStorage.getSlots();
    }

    @Override
    public @NotNull PigmentStack getChemicalInTank(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return PigmentStack.EMPTY;
        }

        IStackKey<?> stack = handlerStorage.getStackBySlot(tank);
        if (stack instanceof PigmentStackType pigmentStack && !pigmentStack.isEmpty())
        {
            return pigmentStack.copyStack();
        }

        return PigmentStack.EMPTY;
    }

    /**
     * 直接设置指定槽位的 Pigment：
     * - EMPTY => 槽位重置为 ItemStackType 空占位；
     * - 非空 => 写入 PigmentStackType。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull PigmentStack stack)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return;
        }

        if (stack.isEmpty())
        {
            handlerStorage.setStackDirectly(tank, new ItemStackType());
        }
        else
        {
            handlerStorage.setStackDirectly(tank, new PigmentStackType(stack.copy()));
        }
    }

    @Override
    public long getTankCapacity(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return 0L;
        }
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, @NotNull PigmentStack stack)
    {
        return true;
    }

    /**
     * 单 tank 插入：
     * - 槽位越界/无效 => 返回原 stack 副本；
     * - 槽位为空或已有 Pigment => 委托统一存储 insert(slot, ...)。
     */
    @Override
    public @NotNull PigmentStack insertChemical(int tank, PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return PigmentStack.EMPTY;
        }
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return stack.copy();
        }
        if (!isValid(tank, stack))
        {
            return stack.copy();
        }

        IStackKey<?> remainingStack = handlerStorage.insert(
                tank,
                new PigmentStackType(stack.copy()),
                action.simulate()
        );

        long remaining = remainingStack.getStackAmount();
        if (remaining <= 0)
        {
            return PigmentStack.EMPTY;
        }
        return new PigmentStack(stack, remaining);
    }

    /**
     * 单 tank 抽取，仅当该槽位当前为 PigmentStackType 且非空时有效。
     */
    @Override
    public @NotNull PigmentStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots() || amount <= 0)
        {
            return PigmentStack.EMPTY;
        }

        IStackKey<?> current = handlerStorage.getStackBySlot(tank);
        if (!(current instanceof PigmentStackType) || current.isEmpty())
        {
            return PigmentStack.EMPTY;
        }

        IStackKey<?> extracted = handlerStorage.extract(tank, amount, action.simulate());
        if (extracted instanceof PigmentStackType pigmentExtract && !pigmentExtract.isEmpty())
        {
            return pigmentExtract.copyStack();
        }
        return PigmentStack.EMPTY;
    }

    /**
     * 无指定 tank 的插入
     */
    @Override
    public @NotNull PigmentStack insertChemical(PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return PigmentStack.EMPTY;
        }

        long remaining = handlerStorage
                .insert(new PigmentStackType(stack.copy()), action.simulate())
                .getStackAmount();

        if (remaining > 0)
        {
            return new PigmentStack(stack, remaining);
        }
        return PigmentStack.EMPTY;
    }

    /**
     * 无指定 tank 的抽取
     */
    @Override
    public @NotNull PigmentStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0)
        {
            return PigmentStack.EMPTY;
        }

        return handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((PigmentStackType) extracts).copyStack())
                .orElse(PigmentStack.EMPTY);
    }

    @Override
    public @NotNull PigmentStack extractChemical(PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return PigmentStack.EMPTY;
        }
        return ((PigmentStackType) handlerStorage.extract(
                new PigmentStackType(stack.copy()),
                action.simulate()
        )).copyStack();
    }

    @Override
    public @NotNull PigmentStack getEmptyStack()
    {
        return PigmentStack.EMPTY;
    }
}