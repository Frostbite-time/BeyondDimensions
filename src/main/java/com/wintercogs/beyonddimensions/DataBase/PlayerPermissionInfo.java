package com.wintercogs.beyonddimensions.DataBase;

import io.netty.buffer.ByteBuf;

public record PlayerPermissionInfo(String name, NetPermissionlevel level)
{

    public static final StreamCodec<ByteBuf,NetPermissionlevel> NET_PERMISSIONLEVEL_STREAM_CODEC = netPermissionlevelStreamCodec();

    // 定义数据包的流编码方式 注册用
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerPermissionInfo> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    PlayerPermissionInfo::name,
                    NET_PERMISSIONLEVEL_STREAM_CODEC,
                    PlayerPermissionInfo::level,
                    PlayerPermissionInfo::new
            );

    static StreamCodec<ByteBuf, NetPermissionlevel> netPermissionlevelStreamCodec()
    {
        return new StreamCodec<ByteBuf, NetPermissionlevel>() {
            public NetPermissionlevel decode(ByteBuf buf) {
                return NetPermissionlevel.valueOf(Utf8String.read(buf,32000));
            }

            public void encode(ByteBuf buf, NetPermissionlevel permissionlevel) {
                Utf8String.write(buf, permissionlevel.toString(), 32000);
            }
        };
    }

}
