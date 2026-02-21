package com.wintercogs.beyonddimensions.Integration.RS;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Optional;

public class RSHelper
{
    public static Optional<ItemStack> fromIStackToItemStack(KeyAmount ka)
    {
        if (ka.key() instanceof ItemStackKey itemStackKey)
            return Optional.of(itemStackKey.copyStackWithCount(ka.amount()));
        return Optional.empty();
    }

    public static Optional<KeyAmount> fromItemStackToIStack(ItemStack stack, long size)
    {
        return Optional.of(new KeyAmount(new ItemStackKey(stack), size));
    }

    public static Optional<FluidStack> fromIStackToFluidStack(KeyAmount ka)
    {
        if (ka.key() instanceof FluidStackKey fluidStackKey)
            return Optional.of(fluidStackKey.copyStackWithCount(ka.amount()));
        return Optional.empty();
    }

    public static Optional<KeyAmount> fromFluidStackToIStack(FluidStack stack, long size)
    {
        return Optional.of(new KeyAmount(new FluidStackKey(stack), size));
    }

}
