package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.InfusionStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import org.jetbrains.annotations.NotNull;

public class InfusionStackTypedHandler implements IInfusionHandler
{

    private final StackTypedHandler handlerStorage;

    public InfusionStackTypedHandler(StackTypedHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 将所有统一存储的槽位视为潜在可用的 Infusion 槽位。
     */
    @Override
    public int getTanks()
    {
        return handlerStorage.getSlots();
    }

    @Override
    public @NotNull InfusionStack getChemicalInTank(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return InfusionStack.EMPTY;
        }

        IStackKey<?> stack = handlerStorage.getStackBySlot(tank);
        if (stack instanceof InfusionStackKey infusionStack && !infusionStack.isEmpty())
        {
            return infusionStack.copyStack();
        }

        return InfusionStack.EMPTY;
    }

    /**
     * 直接设置指定槽位化学品：
     * - EMPTY => 还原为 ItemStackType 空占位；
     * - 非空 => 写入 InfusionStackType。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull InfusionStack stack)
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
            handlerStorage.setStackDirectly(tank, new InfusionStackKey(stack.copy()));
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
    public boolean isValid(int tank, @NotNull InfusionStack stack)
    {
        return true;
    }

    /**
     * 单 tank 插入：
     * - 槽位越界/无效 => 返回原 stack 副本；
     * - 槽位为空或已有 Infusion => 委托统一存储 insert(slot, ...)。
     */
    @Override
    public @NotNull InfusionStack insertChemical(int tank, InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return InfusionStack.EMPTY;
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
                new InfusionStackKey(stack.copy()),
                action.simulate()
        );

        long remaining = remainingStack.getStackAmount();
        if (remaining <= 0)
        {
            return InfusionStack.EMPTY;
        }
        return new InfusionStack(stack, remaining);
    }

    /**
     * 单 tank 抽取，仅当该槽位当前为 InfusionStackType 且非空时有效。
     */
    @Override
    public @NotNull InfusionStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots() || amount <= 0)
        {
            return InfusionStack.EMPTY;
        }

        IStackKey<?> current = handlerStorage.getStackBySlot(tank);
        if (!(current instanceof InfusionStackKey) || current.isEmpty())
        {
            return InfusionStack.EMPTY;
        }

        IStackKey<?> extracted = handlerStorage.extract(tank, amount, action.simulate());
        if (extracted instanceof InfusionStackKey infusionExtract && !infusionExtract.isEmpty())
        {
            return infusionExtract.copyStack();
        }
        return InfusionStack.EMPTY;
    }

    /**
     * 无指定 tank 的插入
     */
    @Override
    public @NotNull InfusionStack insertChemical(InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return InfusionStack.EMPTY;
        }

        long remaining = handlerStorage
                .insert(new InfusionStackKey(stack.copy()), action.simulate())
                .getStackAmount();

        if (remaining > 0)
        {
            return new InfusionStack(stack, remaining);
        }
        return InfusionStack.EMPTY;
    }

    /**
     * 无指定 tank 的抽取
     */
    @Override
    public @NotNull InfusionStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0)
        {
            return InfusionStack.EMPTY;
        }

        return handlerStorage.getTypeIdIndexList(InfusionStackKey.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((InfusionStackKey) extracts).copyStack())
                .orElse(InfusionStack.EMPTY);
    }

    @Override
    public @NotNull InfusionStack extractChemical(InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return InfusionStack.EMPTY;
        }
        return ((InfusionStackKey) handlerStorage.extract(
                new InfusionStackKey(stack.copy()),
                action.simulate()
        )).copyStack();
    }

    @Override
    public @NotNull InfusionStack getEmptyStack()
    {
        return InfusionStack.EMPTY;
    }
}