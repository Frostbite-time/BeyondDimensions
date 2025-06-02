package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.wintercogs.beyonddimensions.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
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
    public static final Map<ResourceLocation, Function<IStackType, Optional<AEKey>>> ISTACK_TO_AEKEY_MAP = new HashMap<>();

    static
    {
        ISTACK_TO_AEKEY_MAP.put(ItemStackType.ID, stackType -> Optional.ofNullable(AEItemKey.of((ItemStack) stackType.copyStack())));
        ISTACK_TO_AEKEY_MAP.put(FluidStackType.ID, stackType -> Optional.ofNullable(AEFluidKey.of((FluidStack) stackType.copyStack())));
    }


    public static Optional<IStackType<?>> fromAEKeyToIStack(AEKey key, long amount)
    {
        if(key instanceof AEItemKey itemKey)
        {
            return Optional.of(new ItemStackType(itemKey.toStack(1), amount));
        }
        else if(key instanceof AEFluidKey fluidKey)
        {
            return Optional.of(new FluidStackType(fluidKey.toStack(1), amount));
        }
        else
        {
            // 通用转换方式
            Object stackKey = key.getPrimaryKey();
            for(IStackType type : StackTypeRegistry.getAllTypes())
            {
                if(type.getSourceClass().isAssignableFrom(stackKey.getClass()))
                {

                    IStackType<?> stack = type.fromObject(stackKey,amount,null);
                    return Optional.of(stack);
                }
            }
            return Optional.empty();
        }
    }


    public static Optional<AEKey> fromIStackToAEKey(IStackType stack)
    {
        if(ISTACK_TO_AEKEY_MAP.containsKey(stack.getTypeId()))
        {
            return ISTACK_TO_AEKEY_MAP.get(stack.getTypeId()).apply(stack);
        }
        return Optional.empty();
    }
}
