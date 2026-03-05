package com.wintercogs.beyonddimensions.integration;

import net.neoforged.fml.ModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModPresence
{
    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private ModPresence()
    {
    }

    public static boolean isLoaded(String modId)
    {
        return CACHE.computeIfAbsent(modId, id -> ModList.get().isLoaded(id));
    }
}
