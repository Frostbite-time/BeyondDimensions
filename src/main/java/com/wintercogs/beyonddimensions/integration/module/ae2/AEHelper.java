package com.wintercogs.beyonddimensions.integration.module.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AEHelper
{
    // 类型转换函数 在任何时间点注册都可
    public static final Map<ResourceLocation, Function<IStackKey<?>, Optional<AEKey>>> ISTACK_TO_AEKEY_MAP = new HashMap<>();
    public static final Map<AEKeyType, Function<AEKey, Optional<IStackKey<?>>>> AEKEY_TO_STACK_TYPE_MAP = new HashMap<>();

    static
    {
        ISTACK_TO_AEKEY_MAP.put(ItemStackKey.ID, stackType -> Optional.ofNullable(AEItemKey.of((ItemStack) stackType.copyStack())));
        ISTACK_TO_AEKEY_MAP.put(FluidStackKey.ID, stackType -> Optional.ofNullable(AEFluidKey.of((FluidStack) stackType.copyStack())));

        AEKEY_TO_STACK_TYPE_MAP.put(AEKeyType.items(), key -> Optional.of(new ItemStackKey(((AEItemKey) key).toStack(1))));
        AEKEY_TO_STACK_TYPE_MAP.put(AEKeyType.fluids(), key -> Optional.of(new FluidStackKey(((AEFluidKey) key).toStack(1))));
    }


    public static Optional<IStackKey<?>> fromAEKeyToIStack(AEKey key)
    {
        if (AEKEY_TO_STACK_TYPE_MAP.containsKey(key.getType()))
        {
            return AEKEY_TO_STACK_TYPE_MAP.get(key.getType()).apply(key);
        }
        return Optional.empty();
    }


    public static Optional<AEKey> fromIStackToAEKey(IStackKey<?> stack)
    {
        if (ISTACK_TO_AEKEY_MAP.containsKey(stack.getTypeId()))
        {
            return ISTACK_TO_AEKEY_MAP.get(stack.getTypeId()).apply(stack);
        }
        return Optional.empty();
    }
}
