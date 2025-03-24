package com.wintercogs.beyonddimensions.Network.Packet.toClient;


import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public record PlayerPermissionInfoPacket(HashMap<UUID, PlayerPermissionInfo> infoMap)
{

    @OnlyIn(Dist.CLIENT)
    private void handle(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        NetControlMenu menu;
        if (!(player.containerMenu instanceof NetControlMenu))
        {
            return;
        }
        menu = (NetControlMenu) player.containerMenu;
        menu.loadPlayerInfo(infoMap());
    }



    public static void handle(PlayerPermissionInfoPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handle(context))
            );
            context.setPacketHandled(true);
        }
    }

    public static void encode(PlayerPermissionInfoPacket packet, FriendlyByteBuf buf)
    {
        // 先写入Map条目数量
        buf.writeInt(packet.infoMap().size());

        // 遍历写入每个键值对
        packet.infoMap().forEach((uuid, info) -> {
            buf.writeUUID(uuid); // 写入UUID
            PlayerPermissionInfo.encode(info, buf); // 复用PlayerPermissionInfo的编码方法
        });
    }

    public static PlayerPermissionInfoPacket decode(FriendlyByteBuf buf)
    {
        HashMap<UUID, PlayerPermissionInfo> map = new HashMap<>();

        // 读取条目数量
        int entryCount = buf.readInt();

        // 循环读取每个键值对
        for (int i = 0; i < entryCount; i++) {
            UUID uuid = buf.readUUID(); // 读取UUID
            PlayerPermissionInfo info = PlayerPermissionInfo.decode(buf); // 复用PlayerPermissionInfo的解码方法
            map.put(uuid, info);
        }

        return new PlayerPermissionInfoPacket(map);
    }

}
