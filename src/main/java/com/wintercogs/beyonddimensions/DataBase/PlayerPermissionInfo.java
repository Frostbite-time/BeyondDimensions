package com.wintercogs.beyonddimensions.DataBase;


import net.minecraft.network.PacketBuffer;

import java.util.UUID;

public class PlayerPermissionInfo {
    private final UUID playerId;
    private final String name;
    private final NetPermissionlevel level;
    public PlayerPermissionInfo(String name, NetPermissionlevel level, UUID playerId) {
        this.name = name;
        this.level = level;
        this.playerId = playerId;
    }
    // 需要添加getter方法
    public String getName() {
        return name;
    }
    public NetPermissionlevel getLevel() {
        return level;
    }

    public UUID getPlayerId()
    {
        return playerId;
    }

    // 改为使用ByteBuf的静态方法
    public static void encode(PlayerPermissionInfo info, PacketBuffer buf) {
        buf.writeString(info.getName());
        buf.writeEnumValue(info.getLevel());
        buf.writeUniqueId(info.playerId);
    }
    public static PlayerPermissionInfo decode(PacketBuffer buf) {
        String name = buf.readString(3000);
        NetPermissionlevel level = buf.readEnumValue(NetPermissionlevel.class);
        UUID playerId = buf.readUniqueId();
        return new PlayerPermissionInfo(name, level, playerId);
    }
}

