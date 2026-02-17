package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import org.jetbrains.annotations.NotNull;

public class SlurryStackTypedHandler implements ISlurryHandler
{

    private final StackTypedHandler handlerStorage;

    public SlurryStackTypedHandler(StackTypedHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 将统一存储中的所有槽位视为潜在的 Slurry 槽位。
     */
    @Override
    public int getTanks()
    {
        return handlerStorage.getSlots();
    }

    @Override
    public @NotNull SlurryStack getChemicalInTank(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return SlurryStack.EMPTY;
        }

        IStackKey<?> stack = handlerStorage.getStackBySlot(tank);
        if (stack instanceof SlurryStackKey slurryStack && !slurryStack.isEmpty())
        {
            return slurryStack.copyStack();
        }

        return SlurryStack.EMPTY;
    }

    /**
     * 直接设置指定槽位的 Slurry：
     * - EMPTY => 槽位重置为 ItemStackType 空占位；
     * - 非空 => 写入 SlurryStackType。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull SlurryStack stack)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return;
        }

        if (stack.isEmpty())
        {
            handlerStorage.setStackDirectly(tank, new ItemStackKey());
        }
        else
        {
            handlerStorage.setStackDirectly(tank, new SlurryStackKey(stack.copy()));
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
    public boolean isValid(int tank, @NotNull SlurryStack stack)
    {
        return true;
    }

    /**
     * 单 tank 插入：
     * - 槽位越界/无效 => 返回原 stack 副本；
     * - 槽位为空或已有 Slurry => 委托统一存储 insert(slot, ...)。
     */
    @Override
    public @NotNull SlurryStack insertChemical(int tank, SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return SlurryStack.EMPTY;
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
                new SlurryStackKey(stack.copy()),
                action.simulate()
        );

        long remaining = remainingStack.getStackAmount();
        if (remaining <= 0)
        {
            return SlurryStack.EMPTY;
        }
        return new SlurryStack(stack, remaining);
    }

    /**
     * 单 tank 抽取，仅当该槽位当前为 SlurryStackType 且非空时有效。
     */
    @Override
    public @NotNull SlurryStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots() || amount <= 0)
        {
            return SlurryStack.EMPTY;
        }

        IStackKey<?> current = handlerStorage.getStackBySlot(tank);
        if (!(current instanceof SlurryStackKey) || current.isEmpty())
        {
            return SlurryStack.EMPTY;
        }

        IStackKey<?> extracted = handlerStorage.extract(tank, amount, action.simulate());
        if (extracted instanceof SlurryStackKey slurryExtract && !slurryExtract.isEmpty())
        {
            return slurryExtract.copyStack();
        }
        return SlurryStack.EMPTY;
    }

    /**
     * 无指定 tank 的插入：交给统一存储按自身策略寻找合适槽位。
     */
    @Override
    public @NotNull SlurryStack insertChemical(SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return SlurryStack.EMPTY;
        }

        long remaining = handlerStorage
                .insert(new SlurryStackKey(stack.copy()), action.simulate())
                .getStackAmount();

        if (remaining > 0)
        {
            return new SlurryStack(stack, remaining);
        }
        return SlurryStack.EMPTY;
    }

    /**
     * 无指定 tank 的抽取
     */
    @Override
    public @NotNull SlurryStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0)
        {
            return SlurryStack.EMPTY;
        }

        return handlerStorage.getTypeIdIndexList(SlurryStackKey.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((SlurryStackKey) extracts).copyStack())
                .orElse(SlurryStack.EMPTY);
    }

    @Override
    public @NotNull SlurryStack extractChemical(SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return SlurryStack.EMPTY;
        }
        return ((SlurryStackKey) handlerStorage.extract(
                new SlurryStackKey(stack.copy()),
                action.simulate()
        )).copyStack();
    }

    @Override
    public @NotNull SlurryStack getEmptyStack()
    {
        return SlurryStack.EMPTY;
    }
}