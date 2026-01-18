package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ChemicalStackTypedHandler implements IChemicalHandler
{

    private static final ResourceLocation CHEM_TYPE = ChemicalStackKey.ID;

    private final StackHandler handlerStorage;

    public ChemicalStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // ---------- 工具：纯 Optional 写法，返回基本类型/哨兵 ----------

    /**
     * 化学品桶的槽位数量（非空的 ChemicalStackKey 槽）。
     */
    private int chemicalCount()
    {
        return handlerStorage.getBucket(CHEM_TYPE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 空桶（EmptyStackKey）的槽位数量。
     */
    private int emptyCount()
    {
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 可视槽位是否处于“化学品区域”（非 Empty 区域）。
     */
    private boolean inChemicalRegion(int visibleSlot)
    {
        int chems = chemicalCount();
        return visibleSlot >= 0 && visibleSlot < chems;
    }

    /**
     * 取化学品桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getChemicalSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(CHEM_TYPE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    /**
     * 取空桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getEmptySlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    /**
     * 将“可视槽位”映射为真实槽位；无效则 -1。
     */
    private int resolveActualIndex(int visibleSlot)
    {
        if (visibleSlot < 0) return -1;
        int chems = chemicalCount();
        if (visibleSlot < chems)
        {
            return getChemicalSlotAt(visibleSlot);
        }
        int rest = visibleSlot - chems;
        return getEmptySlotAt(rest);
    }

    // ================= IChemicalHandler =================

    /**
     * 可视槽位 = 化学品桶 + 空桶；全空时也会 > 0。
     */
    @Override
    public int getChemicalTanks()
    {
        return chemicalCount() + emptyCount();
    }

    /**
     * 高频：直接使用缓存对象并设数量（不 copy）。
     * 若缓存不存在/为空，或该槽处于空区域，则返回 ChemicalStack.EMPTY。
     * 注意：展示量不限制到容量，只要 > 0 即可。
     */
    @Override
    public @NotNull ChemicalStack getChemicalInTank(int slot)
    {
        if (!inChemicalRegion(slot)) return ChemicalStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return ChemicalStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return ChemicalStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof ChemicalStack chem)) return ChemicalStack.EMPTY;
        if (chem.isEmpty()) return ChemicalStack.EMPTY;

        long shown = ka.amount();
        if (shown <= 0) return ChemicalStack.EMPTY;

        chem.setAmount(shown); // 直接改缓存数量
        return chem;
    }

    /**
     * 直接设置指定“可视槽位”的化学品（空区同样支持）。
     */
    @Override
    public void setChemicalInTank(int tank, @NotNull ChemicalStack stack)
    {
        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return;
        handlerStorage.setStackDirectly(actualIndex, new ChemicalStackKey(stack), stack.getAmount());
    }

    @Override
    public long getChemicalTankCapacity(int tank)
    {
        return 64_000L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack)
    {
        // 放宽；最终由底层 insert/extract 决定
        return true;
    }

    /**
     * 指定槽位插入：空区或化学品区都可尝试在该真实槽位插入。
     */
    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action)
    {
        if (stack.isEmpty()) return ChemicalStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return stack.copy();

        KeyAmount remaining = handlerStorage.insert(actualIndex, new ChemicalStackKey(stack), stack.getAmount(), action.simulate());
        long rem = remaining.amount();
        return (rem > 0) ? stack.copyWithAmount(rem) : ChemicalStack.EMPTY;
    }

    /**
     * 指定槽位抽取：仅化学品区可抽；空区恒 EMPTY。
     */
    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        if (amount <= 0) return ChemicalStack.EMPTY;
        if (!inChemicalRegion(tank)) return ChemicalStack.EMPTY;

        int actualIndex = resolveActualIndex(tank);
        if (actualIndex < 0) return ChemicalStack.EMPTY;

        Object out = handlerStorage.extract(actualIndex, amount, action.simulate()).toStack();
        return (out instanceof ChemicalStack cs) ? cs : ChemicalStack.EMPTY;
    }

    /**
     * 聚合插入：不区分具体槽位，交给底层按 Key 合并/分配。
     */
    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action)
    {
        if (stack.isEmpty()) return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(new ChemicalStackKey(stack), stack.getAmount(), action.simulate()).amount();
        return (remaining > 0) ? stack.copyWithAmount(remaining) : ChemicalStack.EMPTY;
    }

    /**
     * 无指定 Key 的抽取：从第一个化学品槽尝试抽取。
     */
    @Override
    public ChemicalStack extractChemical(long amount, Action action)
    {
        if (amount <= 0) return ChemicalStack.EMPTY;

        int firstChemSlot = handlerStorage.getBucket(CHEM_TYPE)
                .map(b -> (b.size() > 0) ? b.get(0) : -1)
                .orElse(-1);
        if (firstChemSlot < 0) return ChemicalStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(firstChemSlot);
        if (ka.isEmpty()) return ChemicalStack.EMPTY;

        Object out = handlerStorage.extract(ka.key(), amount, action.simulate()).toStack();
        return (out instanceof ChemicalStack cs) ? cs : ChemicalStack.EMPTY;
    }

    /**
     * 按 Key 精确抽取（不区分具体槽位）。
     */
    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action)
    {
        if (stack.isEmpty()) return ChemicalStack.EMPTY;
        Object out = handlerStorage.extract(new ChemicalStackKey(stack), stack.getAmount(), action.simulate()).toStack();
        return (out instanceof ChemicalStack cs) ? cs : ChemicalStack.EMPTY;
    }
}

