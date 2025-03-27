package com.wintercogs.beyonddimensions.Network.Packet.toClient;


import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


import java.util.HashMap;
import java.util.UUID;

public class PlayerPermissionInfoPacket implements IMessage
{
    private HashMap<UUID, PlayerPermissionInfo> infoMap = new HashMap<>();

    public PlayerPermissionInfoPacket()
    {
    }

    public PlayerPermissionInfoPacket(HashMap<UUID, PlayerPermissionInfo> infoMap)
    {
        this.infoMap = infoMap;
    }

    public HashMap<UUID, PlayerPermissionInfo> getInfoMap()
    {
        return infoMap;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        infoMap = new HashMap<>();
        int entryCount = buf.readInt();
        for (int i = 0; i < entryCount; i++) {
            // 读取UUID（拆分为两个long）
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            UUID uuid = new UUID(mostSig, leastSig);
            PlayerPermissionInfo info = PlayerPermissionInfo.decode(buf);
            infoMap.put(uuid, info);
        }
    }
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(infoMap.size());
        infoMap.forEach((uuid, info) -> {
            // 写入UUID
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
            // 使用静态编码方法
            PlayerPermissionInfo.encode(info, buf);
        });
    }


    @SideOnly(Side.CLIENT)
    public static class PlayerPermissionInfoPacketHandler implements IMessageHandler<PlayerPermissionInfoPacket, IMessage>
    {
        @Override
        public IMessage onMessage(PlayerPermissionInfoPacket message, MessageContext ctx)
        {
            // 确保在客户端主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;

                NetControlMenu menu;
                if (!(player.openContainer instanceof NetControlMenu))
                {
                    return;
                }
                menu = (NetControlMenu) player.openContainer;
                menu.loadPlayerInfo(message.infoMap);
            });
            return null; // 不需要回复消息
        }
    }

}
