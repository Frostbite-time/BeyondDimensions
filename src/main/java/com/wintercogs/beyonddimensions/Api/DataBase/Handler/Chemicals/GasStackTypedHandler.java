package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GasStackTypedHandler implements IGasHandler
{

    private static final ResourceLocation GAS_TYPE = GasStackKey.ID;

    private final StackHandler handlerStorage;

    public GasStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // ---------- bucket / slot 映射 ----------

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

    // ================= IGasHandler =================

    /**
     * 可视槽位 = Gas 桶 + Empty 桶
     */
    @Override
    public int getTanks()
    {
        return gasCount() + emptyCount();
    }

    /**
     * 返回一个带数量的缓存对象给对方读取
     */
    @Override
    public @NotNull GasStack getChemicalInTank(int tank)
    {
        if (!inGasRegion(tank)) return GasStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return GasStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return GasStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof GasStack gas)) return GasStack.EMPTY;
        if (gas.isEmpty()) return GasStack.EMPTY;

        long shown = ka.amount();
        if (shown <= 0) return GasStack.EMPTY;

        gas.setAmount(shown);
        return gas;
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, @NotNull GasStack stack)
    {
        // 放宽；最终由 insert/extract 决定（与 Fluid 版策略一致）
        return true;
    }

    /**
     * setChemicalInTank：只允许改 “空 / 气体槽”，避免误删其它类型槽位内容。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull GasStack stack)
    {
        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return;

        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        boolean canMutate = current.isEmpty() || current.key() instanceof GasStackKey;
        if (!canMutate)
        {
            return;
        }

        if (stack.isEmpty())
        {
            handlerStorage.setStackDirectly(actualIndex, EmptyStackKey.INSTANCE, 0);
        }
        else
        {
            handlerStorage.setStackDirectly(actualIndex, new GasStackKey(stack), stack.getAmount());
        }
    }

    /**
     * 指定 tank 插入：返回剩余（IGasHandler 约定）
     */
    @Override
    public @NotNull GasStack insertChemical(int tank, @NotNull GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return GasStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return stack.copy();
        if (!isValid(tank, stack)) return stack.copy();

        long requested = stack.getAmount();

        // 优先使用“按槽位插入”（如果您 StackHandler 支持）
        KeyAmount rem;
        try
        {
            rem = handlerStorage.insert(actualIndex, new GasStackKey(stack), requested, action.simulate());
        }
        catch (Throwable t)
        {
            // fallback：如果没有 slot 版本，就退化为全局插入（注意：这会忽略 tank 指定）
            rem = handlerStorage.insert(new GasStackKey(stack), requested, action.simulate());
        }

        long remaining = rem.amount();
        if (remaining <= 0) return GasStack.EMPTY;
        return new GasStack(stack, remaining);
    }

    /**
     * 指定 tank 抽取：返回抽出的量（IGasHandler 约定）
     */
    @Override
    public @NotNull GasStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (amount <= 0) return GasStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return GasStack.EMPTY;

        // 若该槽不是 gas 槽，直接空
        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        if (current.isEmpty() || !(current.key() instanceof GasStackKey)) return GasStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(actualIndex, amount, action.simulate());

        if (extracted.isEmpty()) return GasStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof GasStack gs) ? gs : GasStack.EMPTY;
    }

    /**
     * 无指定 tank 插入：交给底层按 key 合并/分配；返回剩余
     */
    @Override
    public @NotNull GasStack insertChemical(@NotNull GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return GasStack.EMPTY;

        long requested = stack.getAmount();
        long remaining = handlerStorage.insert(new GasStackKey(stack), requested, action.simulate()).amount();

        if (remaining > 0) return new GasStack(stack, remaining);
        return GasStack.EMPTY;
    }

    /**
     * 无指定 tank 抽取：从第一个 Gas 槽尝试抽取；返回抽出的量
     */
    @Override
    public @NotNull GasStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0) return GasStack.EMPTY;

        int firstGasSlot = handlerStorage.getBucket(GAS_TYPE)
                .map(b -> (b.size() > 0) ? b.get(0) : -1)
                .orElse(-1);
        if (firstGasSlot < 0) return GasStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(firstGasSlot);
        if (ka.isEmpty()) return GasStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(firstGasSlot, amount, action.simulate());
        if (extracted.isEmpty()) return GasStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof GasStack gs) ? gs : GasStack.EMPTY;
    }

    /**
     * 按 key 精确抽取：返回抽出的量
     */
    @Override
    public @NotNull GasStack extractChemical(@NotNull GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return GasStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(new GasStackKey(stack), stack.getAmount(), action.simulate(), false);
        if (extracted.isEmpty()) return GasStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof GasStack gs) ? gs : GasStack.EMPTY;
    }

    @Override
    public @NotNull GasStack getEmptyStack()
    {
        return GasStack.EMPTY;
    }
}