package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.InfusionStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
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

    // ================= IInfuseHandler =================

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
    public @NotNull InfusionStack getChemicalInTank(int tank)
    {
        if (!inGasRegion(tank)) return InfusionStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return InfusionStack.EMPTY;

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
    public long getTankCapacity(int tank)
    {
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, @NotNull InfusionStack stack)
    {
        // 放宽；最终由 insert/extract 决定（与 Fluid 版策略一致）
        return true;
    }

    /**
     * setChemicalInTank：只允许改 “空 / 气体槽”，避免误删其它类型槽位内容。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull InfusionStack stack)
    {
        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return;

        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        boolean canMutate = current.isEmpty() || current.key() instanceof InfusionStackKey;
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
            handlerStorage.setStackDirectly(actualIndex, new InfusionStackKey(stack), stack.getAmount());
        }
    }

    /**
     * 指定 tank 插入：返回剩余（IInfuseHandler 约定）
     */
    @Override
    public @NotNull InfusionStack insertChemical(int tank, @NotNull InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return stack.copy();
        if (!isValid(tank, stack)) return stack.copy();

        long requested = stack.getAmount();

        // 优先使用“按槽位插入”（如果您 StackHandler 支持）
        KeyAmount rem;
        try
        {
            rem = handlerStorage.insert(actualIndex, new InfusionStackKey(stack), requested, action.simulate());
        }
        catch (Throwable t)
        {
            // fallback：如果没有 slot 版本，就退化为全局插入（注意：这会忽略 tank 指定）
            rem = handlerStorage.insert(new InfusionStackKey(stack), requested, action.simulate());
        }

        long remaining = rem.amount();
        if (remaining <= 0) return InfusionStack.EMPTY;
        return new InfusionStack(stack, remaining);
    }

    /**
     * 指定 tank 抽取：返回抽出的量（IInfuseHandler 约定）
     */
    @Override
    public @NotNull InfusionStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (amount <= 0) return InfusionStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return InfusionStack.EMPTY;

        // 若该槽不是 gas 槽，直接空
        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        if (current.isEmpty() || !(current.key() instanceof InfusionStackKey)) return InfusionStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(actualIndex, amount, action.simulate());

        if (extracted.isEmpty()) return InfusionStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof InfusionStack gs) ? gs : InfusionStack.EMPTY;
    }

    /**
     * 无指定 tank 插入：交给底层按 key 合并/分配；返回剩余
     */
    @Override
    public @NotNull InfusionStack insertChemical(@NotNull InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;

        long requested = stack.getAmount();
        long remaining = handlerStorage.insert(new InfusionStackKey(stack), requested, action.simulate()).amount();

        if (remaining > 0) return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;
    }

    /**
     * 无指定 tank 抽取：从第一个 Gas 槽尝试抽取；返回抽出的量
     */
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

        KeyAmount extracted = handlerStorage.extract(firstGasSlot, amount, action.simulate());
        if (extracted.isEmpty()) return InfusionStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof InfusionStack gs) ? gs : InfusionStack.EMPTY;
    }

    /**
     * 按 key 精确抽取：返回抽出的量
     */
    @Override
    public @NotNull InfusionStack extractChemical(@NotNull InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(new InfusionStackKey(stack), stack.getAmount(), action.simulate(), false);
        if (extracted.isEmpty()) return InfusionStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof InfusionStack gs) ? gs : InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack getEmptyStack()
    {
        return InfusionStack.EMPTY;
    }
}
