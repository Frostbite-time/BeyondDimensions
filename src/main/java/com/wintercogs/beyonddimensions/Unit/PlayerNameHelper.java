package com.wintercogs.beyonddimensions.Unit;

import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.Level;

public class PlayerNameHelper
{
    // 在线/离线玩家均可查询（优先返回缓存名称）
    public static String getPlayerNameByUUID(UUID uuid, Level infoProvider) {

        MinecraftServer server = infoProvider.getServer();
        // 1. 优先检查在线玩家（即时获取）
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getGameProfile().getName(); // 实时名称可能包含昵称插件修改
        }

        // 2. 若不在线，查询服务端的缓存（ProfileCache）
        GameProfileCache profileCache = server.getProfileCache();
        if (profileCache != null) {
            Optional<GameProfile> profileInfo =
                    profileCache.get(uuid);
            if (profileInfo.isPresent()) {
                return profileInfo.get().getName();
            }
        }

        // 3. 若缓存无记录，返回 null 或特定占位符（可扩展 Mojang API 异步查询）
        return "Unknown";
    }
}
