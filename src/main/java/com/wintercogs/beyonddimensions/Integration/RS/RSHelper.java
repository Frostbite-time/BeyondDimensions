package com.wintercogs.beyonddimensions.Integration.RS;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceTypes;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RSHelper
{
    public static final Map<ResourceLocation, Function<IStackType, Optional<ResourceKey>>> ISTACK_TO_RSKEY_MAP = new HashMap<>();
    public static final Map<ResourceType, BiFunction<ResourceKey, Long, Optional<IStackType<?>>>> RSKEY_TO_STACK_TYPE_MAP = new HashMap<>();

    static
    {
        ISTACK_TO_RSKEY_MAP.put(ItemStackType.ID, stackType -> Optional.ofNullable(ItemResource.ofItemStack((ItemStack)stackType.copyStack())));
        ISTACK_TO_RSKEY_MAP.put(FluidStackType.ID, stackType -> {
            FluidStack stack = (FluidStack) stackType.copyStack();
            return Optional.ofNullable(new FluidResource(stack.getFluid(), stack.getComponentsPatch()));
        });

        RSKEY_TO_STACK_TYPE_MAP.put(ResourceTypes.ITEM, (key, amount) -> Optional.of(new ItemStackType(((ItemResource)key).toItemStack(),amount)));
        RSKEY_TO_STACK_TYPE_MAP.put(ResourceTypes.FLUID, (key, amount) -> {
            FluidResource fluidKey = (FluidResource) key;
            FluidStack stack = new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluidKey.fluid()),1,fluidKey.components());
            return Optional.of(new FluidStackType(stack,amount));
        });
    }


    public static Optional<IStackType<?>> fromRSKeyToIStack(ResourceKey key, long amount)
    {
        if(key instanceof PlatformResourceKey pKey)
        {
            if(RSKEY_TO_STACK_TYPE_MAP.containsKey(pKey.getResourceType()))
            {
                return RSKEY_TO_STACK_TYPE_MAP.get(pKey.getResourceType()).apply(pKey, amount);
            }
        }
        return Optional.empty();
    }


    public static Optional<ResourceKey> fromIStackToRSKey(IStackType stack)
    {
        if(ISTACK_TO_RSKEY_MAP.containsKey(stack.getTypeId()))
        {
            return ISTACK_TO_RSKEY_MAP.get(stack.getTypeId()).apply(stack);
        }
        return Optional.empty();
    }
}
