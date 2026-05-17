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
     * 根据uuid查询玩家名称
     * <p>优先查询在线玩家数据</p>
     * <p>其次是缓存数据</p>
     *
     * @param uuid         玩家id
     * @param infoProvider 提供数据的服务器
     * @return 玩家名称
     */
    public static String getPlayerNameByUUID(UUID uuid, MinecraftServer infoProvider)
    {

        // 在线查询
        ServerPlayer onlinePlayer = infoProvider.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null)
        {
            return onlinePlayer.getGameProfile().name();
        }

        // 缓存查询
        var services = infoProvider.services();
        Optional<GameProfile> profileInfo = services.profileResolver().fetchById(uuid);
        if (profileInfo.isPresent())
        {
            GameProfile profile = profileInfo.get();
            return profile.name();
        }

        return UNKNOWN_PLAYER;
    }
}
