package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AEHelper
{
    // 类型转换函数 在任何时间点注册都可
    public static final Map<ResourceLocation, Function<IStackKey, Optional<AEKey>>> ISTACK_TO_AEKEY_MAP = new HashMap<>();
    public static final Map<AEKeyType, BiFunction<AEKey, Long, Optional<IStackKey<?>>>> AEKEY_TO_STACK_TYPE_MAP = new HashMap<>();

    static
    {
        ISTACK_TO_AEKEY_MAP.put(ItemStackKey.ID, stackType -> Optional.ofNullable(AEItemKey.of((ItemStack) stackType.copyStack())));
        ISTACK_TO_AEKEY_MAP.put(FluidStackKey.ID, stackType -> Optional.ofNullable(AEFluidKey.of((FluidStack) stackType.copyStack())));

        AEKEY_TO_STACK_TYPE_MAP.put(AEKeyType.items(), (key, amount) -> Optional.of(new ItemStackKey(((AEItemKey) key).toStack(1), amount)));
        AEKEY_TO_STACK_TYPE_MAP.put(AEKeyType.fluids(), (key, amount) -> Optional.of(new FluidStackKey(((AEFluidKey) key).toStack(1), amount)));
    }


    public static Optional<IStackKey<?>> fromAEKeyToIStack(AEKey key, long amount)
    {
        if (AEKEY_TO_STACK_TYPE_MAP.containsKey(key.getType()))
        {
            return AEKEY_TO_STACK_TYPE_MAP.get(key.getType()).apply(key, amount);
        }
        return Optional.empty();
    }


    public static Optional<AEKey> fromIStackToAEKey(IStackKey stack)
    {
        if (ISTACK_TO_AEKEY_MAP.containsKey(stack.getTypeId()))
        {
            return ISTACK_TO_AEKEY_MAP.get(stack.getTypeId()).apply(stack);
        }
        return Optional.empty();
    }
}
