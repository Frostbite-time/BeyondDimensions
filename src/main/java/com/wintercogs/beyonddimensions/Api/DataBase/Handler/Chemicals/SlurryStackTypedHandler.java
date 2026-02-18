package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlurryStackTypedHandler implements ISlurryHandler
{

    private static final ResourceLocation GAS_TYPE = SlurryStackKey.ID;

    private final StackHandler handlerStorage;

    public SlurryStackTypedHandler(StackHandler handlerStorage)
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

    // ================= ISlurryHandler =================

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
    public @NotNull SlurryStack getChemicalInTank(int tank)
    {
        if (!inGasRegion(tank)) return SlurryStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return SlurryStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return SlurryStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof SlurryStack gas)) return SlurryStack.EMPTY;
        if (gas.isEmpty()) return SlurryStack.EMPTY;

        long shown = ka.amount();
        if (shown <= 0) return SlurryStack.EMPTY;

        gas.setAmount(shown);
        return gas;
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, @NotNull SlurryStack stack)
    {
        // 放宽；最终由 insert/extract 决定（与 Fluid 版策略一致）
        return true;
    }

    /**
     * setChemicalInTank：只允许改 “空 / 气体槽”，避免误删其它类型槽位内容。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull SlurryStack stack)
    {
        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return;

        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        boolean canMutate = current.isEmpty() || current.key() instanceof SlurryStackKey;
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
            handlerStorage.setStackDirectly(actualIndex, new SlurryStackKey(stack), stack.getAmount());
        }
    }

    /**
     * 指定 tank 插入：返回剩余（ISlurryHandler 约定）
     */
    @Override
    public @NotNull SlurryStack insertChemical(int tank, @NotNull SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return SlurryStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return stack.copy();
        if (!isValid(tank, stack)) return stack.copy();

        long requested = stack.getAmount();

        // 优先使用“按槽位插入”（如果您 StackHandler 支持）
        KeyAmount rem;
        try
        {
            rem = handlerStorage.insert(actualIndex, new SlurryStackKey(stack), requested, action.simulate());
        }
        catch (Throwable t)
        {
            // fallback：如果没有 slot 版本，就退化为全局插入（注意：这会忽略 tank 指定）
            rem = handlerStorage.insert(new SlurryStackKey(stack), requested, action.simulate());
        }

        long remaining = rem.amount();
        if (remaining <= 0) return SlurryStack.EMPTY;
        return new SlurryStack(stack, remaining);
    }

    /**
     * 指定 tank 抽取：返回抽出的量（ISlurryHandler 约定）
     */
    @Override
    public @NotNull SlurryStack extractChemical(int tank, long amount, @NotNull Action action)
    {
        if (amount <= 0) return SlurryStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0 || actualIndex >= handlerStorage.getSlots()) return SlurryStack.EMPTY;

        // 若该槽不是 gas 槽，直接空
        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        if (current.isEmpty() || !(current.key() instanceof SlurryStackKey)) return SlurryStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(actualIndex, amount, action.simulate());

        if (extracted.isEmpty()) return SlurryStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof SlurryStack gs) ? gs : SlurryStack.EMPTY;
    }

    /**
     * 无指定 tank 插入：交给底层按 key 合并/分配；返回剩余
     */
    @Override
    public @NotNull SlurryStack insertChemical(@NotNull SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return SlurryStack.EMPTY;

        long requested = stack.getAmount();
        long remaining = handlerStorage.insert(new SlurryStackKey(stack), requested, action.simulate()).amount();

        if (remaining > 0) return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;
    }

    /**
     * 无指定 tank 抽取：从第一个 Gas 槽尝试抽取；返回抽出的量
     */
    @Override
    public @NotNull SlurryStack extractChemical(long amount, @NotNull Action action)
    {
        if (amount <= 0) return SlurryStack.EMPTY;

        int firstGasSlot = handlerStorage.getBucket(GAS_TYPE)
                .map(b -> (b.size() > 0) ? b.get(0) : -1)
                .orElse(-1);
        if (firstGasSlot < 0) return SlurryStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(firstGasSlot);
        if (ka.isEmpty()) return SlurryStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(firstGasSlot, amount, action.simulate());
        if (extracted.isEmpty()) return SlurryStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof SlurryStack gs) ? gs : SlurryStack.EMPTY;
    }

    /**
     * 按 key 精确抽取：返回抽出的量
     */
    @Override
    public @NotNull SlurryStack extractChemical(@NotNull SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty()) return SlurryStack.EMPTY;

        KeyAmount extracted = handlerStorage.extract(new SlurryStackKey(stack), stack.getAmount(), action.simulate(), false);
        if (extracted.isEmpty()) return SlurryStack.EMPTY;

        Object out = extracted.toStack();
        return (out instanceof SlurryStack gs) ? gs : SlurryStack.EMPTY;
    }

    @Override
    public @NotNull SlurryStack getEmptyStack()
    {
        return SlurryStack.EMPTY;
    }
}