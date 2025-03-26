package com.wintercogs.beyonddimensions.Unit;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.World;


import java.util.Optional;
import java.util.UUID;

public class PlayerNameHelper
{

    public static String getPlayerNameByUUID(UUID uuid, World world) {
        MinecraftServer server = world.getMinecraftServer();

        return Optional.ofNullable(server.getPlayerList().getPlayerByUUID(uuid))
                .map(EntityPlayerMP::getName)
                .orElseGet(() -> getCachedName(uuid, server));
    }
    private static String getCachedName(UUID uuid, MinecraftServer server) {
        PlayerProfileCache cache = server.getPlayerProfileCache();
        return Optional.ofNullable(cache)
                .flatMap(c -> Optional.ofNullable(c.getProfileByUUID(uuid)))
                .map(GameProfile::getName)
                .orElse("Unknown");
    }

}
