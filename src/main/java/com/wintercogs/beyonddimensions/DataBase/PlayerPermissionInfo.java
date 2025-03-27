package com.wintercogs.beyonddimensions.DataBase;


import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PlayerPermissionInfo {
    private final String name;
    private final NetPermissionlevel level;
    public PlayerPermissionInfo(String name, NetPermissionlevel level) {
        this.name = name;
        this.level = level;
    }
    // 需要添加getter方法
    public String getName() {
        return name;
    }
    public NetPermissionlevel getLevel() {
        return level;
    }
    // 改为使用ByteBuf的静态方法
    public static void encode(PlayerPermissionInfo info, ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, info.name); // 1.12.2字符串写入方式
        buf.writeInt(info.level.ordinal()); // 通过枚举序数存储
    }
    public static PlayerPermissionInfo decode(ByteBuf buf) {
        String name = ByteBufUtils.readUTF8String(buf);
        NetPermissionlevel level = NetPermissionlevel.values()[buf.readInt()];
        return new PlayerPermissionInfo(name, level);
    }
}

