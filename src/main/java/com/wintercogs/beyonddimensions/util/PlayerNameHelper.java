package com.wintercogs.beyonddimensions.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public class PlayerNameHelper
{
    public static final String UNKNOWN_PLAYER = "Unknown";

    /**
     * 从uuid查询玩家名
     */
    public static String getPlayerNameByUUID(UUID uuid, MinecraftServer infoProvider)
    {

        // 优先检查在线玩家
        ServerPlayer onlinePlayer = infoProvider.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null)
        {
            return onlinePlayer.getGameProfile().name();
        }

        // 若不在线，查询服务端的 profile resolver 缓存
        var services = infoProvider.services();
        Optional<GameProfile> profileInfo = services.profileResolver().fetchById(uuid);
        if (profileInfo.isPresent())
        {
            GameProfile profile = profileInfo.get();
            return profile.name();
        }

        // 无记录，返回未知名称
        return UNKNOWN_PLAYER;
    }
}
