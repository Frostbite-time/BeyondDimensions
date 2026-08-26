package com.wintercogs.beyonddimensions.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class ServerConfigRuntime
{
    private ServerConfigRuntime()
    {
    }


    public static long fragmentTransferTime = 3600;
    public static int crystalGenerateTime = 600;
    private static volatile Set<ResourceLocation> interfaceBlockedInputStackTypes = Set.of();

    public static void setInterfaceBlockedInputStackTypes(Collection<? extends String> configuredIds)
    {
        Set<ResourceLocation> parsedIds = new HashSet<>();
        for (String configuredId : configuredIds)
        {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            if (id != null)
            {
                parsedIds.add(id);
            }
        }
        interfaceBlockedInputStackTypes = Set.copyOf(parsedIds);
    }

    public static boolean hasInterfaceInputTypeRestrictions()
    {
        return !interfaceBlockedInputStackTypes.isEmpty();
    }

    public static boolean isInterfaceInputTypeBlocked(ResourceLocation typeId)
    {
        return interfaceBlockedInputStackTypes.contains(typeId);
    }
}
