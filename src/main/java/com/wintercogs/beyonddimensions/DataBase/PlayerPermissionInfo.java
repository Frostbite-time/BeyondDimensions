package com.wintercogs.beyonddimensions.DataBase;

import net.minecraft.network.FriendlyByteBuf;

public record PlayerPermissionInfo(String name, NetPermissionlevel level)
{

    public static void encode(PlayerPermissionInfo info , FriendlyByteBuf buf) {
        buf.writeUtf(info.name()); // 写入字符串
        buf.writeEnum(info.level()); // 写入枚举值
    }

    public static PlayerPermissionInfo decode(FriendlyByteBuf buf)
    {
        String name = buf.readUtf(); // 读取字符串
        NetPermissionlevel level = buf.readEnum(NetPermissionlevel.class); // 读取枚举值
        return new PlayerPermissionInfo(name, level);
    }

}
