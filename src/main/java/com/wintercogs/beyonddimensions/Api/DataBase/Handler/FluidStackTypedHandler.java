package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

public class FluidStackTypedHandler implements IFluidHandler
{

    private final StackTypedHandler handlerStorage;

    public FluidStackTypedHandler(StackTypedHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 将统一存储中的所有槽位视为潜在的流体槽位。
     */
    @Override
    public int getTanks()
    {
        return handlerStorage.getSlots();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return FluidStack.EMPTY;
        }

        IStackKey<?> stack = handlerStorage.getStackBySlot(tank);
        if (stack instanceof FluidStackType fluidStackType && !fluidStackType.isEmpty())
        {
            return fluidStackType.copyStack();
        }

        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return 0;
        }
        // 对外宣称的单 tank 容量；实际内部容量由 StackTypedHandler 控制
        return 64_000;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack fluidStack)
    {
        if (tank < 0 || tank >= handlerStorage.getSlots())
        {
            return false;
        }
        return true;
    }

    /**
     * 无指定 tank 的填充，由统一存储自己决定放到哪些槽里。
     */
    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction)
    {
        if (fluidStack.isEmpty())
        {
            return 0;
        }

        int requested = fluidStack.getAmount();
        long remaining = handlerStorage
                .insert(new FluidStackType(fluidStack.copy()), fluidAction.simulate())
                .getStackAmount();

        int actuallyFilled = requested - (int) remaining;
        return Math.max(actuallyFilled, 0);
    }

    /**
     * 按指定流体类型抽取，由统一存储自己决定从哪些槽扣。
     */
    @Override
    public @NotNull FluidStack drain(FluidStack fluidStack, FluidAction fluidAction)
    {
        if (fluidStack.isEmpty())
        {
            return FluidStack.EMPTY;
        }

        IStackKey<?> extracted = handlerStorage.extract(
                new FluidStackType(fluidStack.copy()),
                fluidAction.simulate()
        );

        if (extracted instanceof FluidStackType fluidExtract && !fluidExtract.isEmpty())
        {
            return fluidExtract.copyStack();
        }
        return FluidStack.EMPTY;
    }

    /**
     * 按数量抽取：从第一个流体槽位开始，
     * 以该槽中的流体类型构造“请求堆叠”，让存储进行扣减。
     */
    @Override
    public @NotNull FluidStack drain(int count, FluidAction fluidAction)
    {
        if (count <= 0)
        {
            return FluidStack.EMPTY;
        }

        return handlerStorage.getTypeIdIndexList(FluidStackType.ID)
                .map(slots -> slots.get(0))                     // 第一个流体槽位
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copyWithCount(count))
                .map(stack -> handlerStorage.extract(stack, fluidAction.simulate()))
                .map(extracts -> ((FluidStackType) extracts).copyStack())
                .orElse(FluidStack.EMPTY);
    }
}