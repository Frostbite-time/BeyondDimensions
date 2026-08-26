package com.wintercogs.beyonddimensions.config;

import net.minecraft.resources.Identifier;

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
    private static volatile Set<Identifier> interfaceBlockedInputStackTypes = Set.of();

    public static void setInterfaceBlockedInputStackTypes(Collection<? extends String> configuredIds)
    {
        Set<Identifier> parsedIds = new HashSet<>();
        for (String configuredId : configuredIds)
        {
            Identifier id = Identifier.tryParse(configuredId);
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

    public static boolean isInterfaceInputTypeBlocked(Identifier typeId)
    {
        return interfaceBlockedInputStackTypes.contains(typeId);
    }
}
