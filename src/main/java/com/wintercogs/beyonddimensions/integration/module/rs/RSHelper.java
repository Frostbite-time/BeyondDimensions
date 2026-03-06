package com.wintercogs.beyonddimensions.integration.module.rs;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceTypes;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RSHelper
{
    public static final Map<ResourceLocation, Function<IStackKey<?>, Optional<ResourceKey>>> ISTACK_TO_RSKEY_MAP = new HashMap<>();
    public static final Map<ResourceType, Function<ResourceKey, Optional<IStackKey<?>>>> RSKEY_TO_STACK_TYPE_MAP = new HashMap<>();

    static
    {
        ISTACK_TO_RSKEY_MAP.put(ItemStackKey.ID, stackType -> Optional.of(ItemResource.ofItemStack((ItemStack) stackType.copyStack())));
        ISTACK_TO_RSKEY_MAP.put(FluidStackKey.ID, stackType -> {
            FluidStack stack = (FluidStack) stackType.copyStack();
            return Optional.of(new FluidResource(stack.getFluid(), stack.getComponentsPatch()));
        });

        RSKEY_TO_STACK_TYPE_MAP.put(ResourceTypes.ITEM, key -> Optional.of(new ItemStackKey(((ItemResource) key).toItemStack())));
        RSKEY_TO_STACK_TYPE_MAP.put(ResourceTypes.FLUID, key -> {
            FluidResource fluidKey = (FluidResource) key;
            FluidStack stack = new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluidKey.fluid()), 1, fluidKey.components());
            return Optional.of(new FluidStackKey(stack));
        });
    }


    public static Optional<IStackKey<?>> fromRSKeyToIStack(ResourceKey key)
    {
        if (key instanceof PlatformResourceKey pKey)
        {
            if (RSKEY_TO_STACK_TYPE_MAP.containsKey(pKey.getResourceType()))
            {
                return RSKEY_TO_STACK_TYPE_MAP.get(pKey.getResourceType()).apply(pKey);
            }
        }
        return Optional.empty();
    }


    public static Optional<ResourceKey> fromIStackToRSKey(IStackKey<?> stack)
    {
        if (ISTACK_TO_RSKEY_MAP.containsKey(stack.getTypeId()))
        {
            return ISTACK_TO_RSKEY_MAP.get(stack.getTypeId()).apply(stack);
        }
        return Optional.empty();
    }
}
