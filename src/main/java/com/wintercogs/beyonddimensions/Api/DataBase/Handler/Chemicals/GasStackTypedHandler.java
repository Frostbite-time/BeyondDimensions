package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import org.jetbrains.annotations.NotNull;

public class GasStackTypedHandler implements IGasHandler
{

    /**
     * 统一存储后端
     */
    private final StackHandler handlerStorage;

    public GasStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 将所有槽位视为潜在可用的气体槽位。
     */
    @Override
    public int getTanks()
    {
        return handlerStorage.getSlots();
    }

    @Override
    public @NotNull GasStack getChemicalInTank(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return GasStack.EMPTY;
        }

        IStackKey<?> stack = handlerStorage.getStackBySlot(tank);
        if (stack instanceof GasStackKey gasStack && !gasStack.isEmpty())
        {
            // 返回一份气体副本
            return gasStack.copyStack();
        }

        // 槽位不是气体类型或者为空，视为 EMPTY
        return GasStack.EMPTY;
    }

    /**
     * 直接设置指定槽位的化学品：
     * - 若传入 EMPTY，则将该槽位重置为 ItemStackType 空占位；
     * - 否则将其设置为 GasStackType。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull GasStack stack)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return;
        }

        if (stack.isEmpty())
        {
            // 清空槽位，恢复成通用空占位
            handlerStorage.setStackDirectly(tank, new ItemStackKey());
        }
        else
        {
            handlerStorage.setStackDirectly(tank, new GasStackKey(stack.copy()));
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
    public boolean isValid(int tank, @NotNull GasStack stack)
    {
        return true;
    }

    /**
     * 单 tank 插入气体。
     * - 槽位越界：返回原 stack 副本；
     * - 槽位里是非气体且非空：不允许覆盖，返回原 stack 副本；
     * - 槽位为空或气体：委托给统一存储的 insert(slot, ...)。
     */
    @Override
    public @NotNull GasStack insertChemical(int tank, GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return GasStack.EMPTY;
        }
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return stack.copy();
        }
        if (!isValid(tank, stack))
        {
            return stack.copy();
        }

        // 统一存储会处理“空占位 -> GasStackType”的转化和索引更新
        IStackKey<?> remainingStack = handlerStorage.insert(tank, new GasStackKey(stack.copy()), action.simulate());

        long remaining = remainingStack.getStackAmount();
        if (remaining <= 0)
        {
            return GasStack.EMPTY;
        }
        return new GasStack(stack, remaining);
    }

    /**
     * 单 tank 抽取气体。
     * 仅当该槽位当前是 GasStackType 且非空时才执行抽取。
     */
    @Override
    public @NotNull GasStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots() || amount <= 0)
        {
            return GasStack.EMPTY;
        }

        IStackKey<?> current = handlerStorage.getStackBySlot(tank);
        if (!(current instanceof GasStackKey) || current.isEmpty())
        {
            return GasStack.EMPTY;
        }

        IStackKey<?> extracted = handlerStorage.extract(tank, amount, action.simulate());
        if (extracted instanceof GasStackKey gasExtract && !gasExtract.isEmpty())
        {
            return gasExtract.copyStack();
        }
        return GasStack.EMPTY;
    }

    /**
     * 无指定 tank 的插入
     */
    @Override
    public @NotNull GasStack insertChemical(GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return GasStack.EMPTY;
        }

        long remaining = handlerStorage
                .insert(new GasStackKey(stack.copy()), action.simulate())
                .getStackAmount();

        if (remaining > 0)
        {
            return new GasStack(stack, remaining);
        }
        return GasStack.EMPTY;
    }

    /**
     * 无指定 tank 的抽取
     */
    @Override
    public @NotNull GasStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0)
        {
            return GasStack.EMPTY;
        }

        return handlerStorage.getTypeIdIndexList(GasStackKey.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((GasStackKey) extracts).copyStack())
                .orElse(GasStack.EMPTY);
    }

    @Override
    public @NotNull GasStack extractChemical(GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
        {
            return GasStack.EMPTY;
        }
        return ((GasStackKey) handlerStorage.extract(new GasStackKey(stack.copy()), action.simulate())).copyStack();
    }

    @Override
    public @NotNull GasStack getEmptyStack()
    {
        return GasStack.EMPTY;
    }
}