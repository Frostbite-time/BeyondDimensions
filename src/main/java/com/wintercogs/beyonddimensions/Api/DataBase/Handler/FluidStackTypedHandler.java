package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class FluidStackTypedHandler implements IFluidHandler
{

    private static final ResourceLocation FLUID_TYPE = FluidStackKey.ID;

    private final StackHandler handlerStorage;

    public FluidStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // ---------- 工具方法：纯 Optional，返回基本类型/哨兵值 ----------

    /**
     * Fluid 桶的槽位数量（非空的 FluidStackKey 槽）。
     */
    private int fluidCount()
    {
        return handlerStorage.getBucket(FLUID_TYPE)
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
     * 可视槽位是否处于 Fluid 区域（非 Empty 区域）。
     */
    private boolean inFluidRegion(int visibleSlot)
    {
        int fluids = fluidCount();
        return visibleSlot >= 0 && visibleSlot < fluids;
    }

    /**
     * 取 Fluid 桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getFluidSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(FLUID_TYPE)
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
        int fluids = fluidCount();
        if (visibleSlot < fluids)
        {
            return getFluidSlotAt(visibleSlot);
        }
        int rest = visibleSlot - fluids;
        return getEmptySlotAt(rest);
    }

    // ================= IFluidHandler =================

    /**
     * 可视槽位 = Fluid 桶 + 空桶；全空时也会 > 0。
     */
    @Override
    public int getTanks()
    {
        return fluidCount() + emptyCount();
    }

    /**
     * 高频：直接使用缓存对象并设数量（不 copy）。
     * 若缓存不存在/为空，或该槽处于空区域，则返回 FluidStack.EMPTY。
     * 注意：对外展示量不限制到 tank 容量（与 Item 视图一致策略）。
     */
    @Override
    public @NotNull FluidStack getFluidInTank(int slot)
    {
        if (!inFluidRegion(slot)) return FluidStack.EMPTY;

        int actualIndex = resolveActualIndex(slot);
        if (actualIndex < 0) return FluidStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        if (ka.isEmpty()) return FluidStack.EMPTY;

        Object cached = handlerStorage.getOutStackByKey(ka.key());
        if (!(cached instanceof FluidStack fluid)) return FluidStack.EMPTY;
        if (fluid.isEmpty()) return FluidStack.EMPTY; // 避免对空栈 setAmount

        int shown = BDMath.clampLongToInt(ka.amount());
        if (shown <= 0) return FluidStack.EMPTY;

        fluid.setAmount(shown); // 直接改缓存数量
        return fluid;
    }

    @Override
    public int getTankCapacity(int tank)
    {
        return 64000;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack fluidStack)
    {
        // 放宽；最终由 insert/extract 决定
        return true;
    }

    /**
     * 聚合填充：不区分具体 tank，交给底层存储按 Key 合并/分配。
     */
    @Override
    public int fill(FluidStack fluidStack, @NotNull FluidAction action)
    {
        if (fluidStack.isEmpty()) return 0;
        int requested = fluidStack.getAmount();
        long remaining = handlerStorage.insert(new FluidStackKey(fluidStack), requested, action.simulate()).amount();
        return requested - BDMath.clampLongToInt(remaining); // 实际插入量
    }

    /**
     * 按 Key 精确抽取（不区分具体 tank）。
     */
    @Override
    public @NotNull FluidStack drain(FluidStack fluidStack, FluidAction action)
    {
        if (fluidStack.isEmpty()) return FluidStack.EMPTY;
        Object out = handlerStorage.extract(new FluidStackKey(fluidStack), fluidStack.getAmount(), action.simulate(), false).toStack();
        return (out instanceof FluidStack fs) ? fs : FluidStack.EMPTY;
    }

    /**
     * 无指定 Key 的抽取：从第一个 Fluid 槽尝试抽取。
     */
    @Override
    public @NotNull FluidStack drain(int amount, FluidAction action)
    {
        if (amount <= 0) return FluidStack.EMPTY;

        int firstFluidSlot = handlerStorage.getBucket(FLUID_TYPE)
                .map(b -> (b.size() > 0) ? b.get(0) : -1)
                .orElse(-1);
        if (firstFluidSlot < 0) return FluidStack.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(firstFluidSlot);
        if (ka.isEmpty()) return FluidStack.EMPTY;

        Object out = handlerStorage.extract(ka.key(), amount, action.simulate(), false).toStack();
        return (out instanceof FluidStack fs) ? fs : FluidStack.EMPTY;
    }
}