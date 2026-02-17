package com.wintercogs.beyonddimensions.Integration.RS;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Optional;

public class RSHelper
{
    public static Optional<ItemStack> fromIStackToItemStack(IStackKey<?> stackType)
    {
        if (stackType instanceof ItemStackKey itemStackKey)
            return Optional.of(itemStackKey.getStack());
        return Optional.empty();
    }

    public static Optional<ItemStackKey> fromItemStackToIStack(ItemStack stack, long size)
    {
        return Optional.of(new ItemStackKey(stack, size));
    }

    public static Optional<FluidStack> fromIStackToFluidStack(IStackKey<?> stackType)
    {
        if (stackType instanceof FluidStackKey fluidStackKey)
            return Optional.of(fluidStackKey.getStack());
        return Optional.empty();
    }

    public static Optional<FluidStackKey> fromFluidStackToIStack(FluidStack stack, long size)
    {
        return Optional.of(new FluidStackKey(stack, size));
    }

}
