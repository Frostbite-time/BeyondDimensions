package com.wintercogs.beyonddimensions.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public class PlayerNameHelper
{
    // 在线/离线玩家均可查询（优先返回缓存名称）
    public static String getPlayerNameByUUID(UUID uuid, MinecraftServer infoProvider)
    {

        MinecraftServer server = infoProvider;
        // 1. 优先检查在线玩家（即时获取）
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null)
        {
            return onlinePlayer.getGameProfile().name();
        }

        // 2. 若不在线，查询服务端的 profile resolver 缓存
        var services = server.services();
        Optional<GameProfile> profileInfo = services.profileResolver().fetchById(uuid);
        if (profileInfo.isPresent())
        {
            GameProfile profile = profileInfo.get();
            return profile.name();
        }

        // 3. 若缓存无记录，返回 null 或特定占位符（可扩展 Mojang API 异步查询）
        return "Unknown";
    }
}
