package com.wintercogs.beyonddimensions.integration.module.rs;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RSHelper
{
    public static final Map<ResourceLocation, Function<IStackKey<?>, Optional<ResourceKey>>> ISTACK_TO_RSKEY_MAP = new HashMap<>();
    public static final Map<ResourceType, Function<ResourceKey, Optional<IStackKey<?>>>> RSKEY_TO_STACK_TYPE_MAP = new HashMap<>();

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
