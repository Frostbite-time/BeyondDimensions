package com.wintercogs.beyonddimensions.Integration.RS;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Optional;

public class RSHelper
{
    public static Optional<ItemStack> fromIStackToItemStack(IStackType<?> stackType)
    {
        if (stackType instanceof ItemStackType itemStackType)
            return Optional.of(itemStackType.getStack());
        return Optional.empty();
    }

    public static Optional<ItemStackType> fromItemStackToIStack(ItemStack stack, long size)
    {
        return Optional.of(new ItemStackType(stack, size));
    }

    public static Optional<FluidStack> fromIStackToFluidStack(IStackType<?> stackType)
    {
        if (stackType instanceof FluidStackType fluidStackType)
            return Optional.of(fluidStackType.getStack());
        return Optional.empty();
    }

    public static Optional<FluidStackType> fromFluidStackToIStack(FluidStack stack, long size)
    {
        return Optional.of(new FluidStackType(stack, size));
    }

}
