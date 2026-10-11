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

    // 网络接口
    public static volatile boolean interfaceReceive = true;
    public static volatile boolean interfaceOutput = true;
    public static volatile boolean interfacePushOut = true;
    public static volatile int interfaceCapacity = 27;
    private static volatile Set<ResourceLocation> interfaceBlockedInputStackTypes = Set.of();

    // 时空材料
    public static volatile long fragmentTransferTime = 3600;
    public static volatile int crystalGenerateTime = 600;

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
