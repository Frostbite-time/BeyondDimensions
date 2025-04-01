package com.wintercogs.beyonddimensions.Gui.Sync;

import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.server.FMLServerHandler;

import java.io.IOException;
import java.util.*;

public class PlayerPermissionsSync extends ValueSyncHandler<List<PlayerPermissionInfo>>
{
    List<PlayerPermissionInfo> permissions;
    List<PlayerPermissionInfo> cache;

    DimensionsNet net;

    public PlayerPermissionsSync(List<PlayerPermissionInfo> permissions)
    {
        this.permissions = permissions;
        cache = new ArrayList<>();
    }

    // 仅用于服务端
    public void setNet(DimensionsNet net)
    {
        this.net = net;
    }


    @Override
    public void setValue(List<PlayerPermissionInfo> playerPermissionInfo, boolean setSource, boolean sync)
    {
        this.cache = playerPermissionInfo;
        if (setSource) {
            permissions = playerPermissionInfo;
        }
        if (sync) {
            if (!NetworkUtils.isClient())
            {
                syncToClient(0, this::write);
            }
        }
        onValueChanged();
    }

    @Override
    public boolean updateCacheFromSource(boolean init)
    {

        if(!NetworkUtils.isClient()&& net != null)
        {
            World world;
//            if(FMLServerHandler.instance()!= null
//                    &&FMLServerHandler.instance().getServer() != null)
//                world = FMLServerHandler.instance().getServer().getEntityWorld();
//            else
            world = Minecraft.getMinecraft().getIntegratedServer().getEntityWorld();
            this.permissions = new ArrayList<>(net.getPlayerPermissionInfoMap(world).values());
        }



        if(cache.size() != permissions.size())
        {
            if (!NetworkUtils.isClient())
            {
                syncToClient(0, this::write);
            }
            cache = permissions;
            return true;
        }
        else
        {
            boolean changed = false;


            // 先比较列表长度
            if (permissions.size() != cache.size()) {
                changed = true;
            } else {
                // 创建临时映射来加速查找
                Map<UUID, PlayerPermissionInfo> cacheMap = new HashMap<>();
                for (PlayerPermissionInfo info : cache) {
                    cacheMap.put(info.getPlayerId(), info);
                }
                // 遍历权限列表检查每个元素
                for (PlayerPermissionInfo perm : permissions) {
                    PlayerPermissionInfo cached = cacheMap.get(perm.getPlayerId());

                    // 检查是否存在且字段完全匹配
                    if (cached == null
                            || !perm.getName().equals(cached.getName())
                            || perm.getLevel() != cached.getLevel()) {
                        changed = true;
                        break;
                    }
                }
            }


            if(changed)
            {
                if (!NetworkUtils.isClient())
                {
                    syncToClient(0, this::write);
                }

                cache = permissions;
                return true;
            }
        }

        return false;
    }

    @Override
    public void write(PacketBuffer packetBuffer) throws IOException
    {
        packetBuffer.writeVarInt(permissions.size());
        permissions.forEach(
                playerPermissionInfo -> {
                    PlayerPermissionInfo.encode(playerPermissionInfo, packetBuffer);
                }
        );
    }

    @Override
    public void read(PacketBuffer packetBuffer) throws IOException
    {
        int length = packetBuffer.readVarInt();
        this.permissions = new ArrayList<>(length);
        for(int i = 0; i < length; i++)
        {
            this.permissions.add(PlayerPermissionInfo.decode(packetBuffer));
        }
    }

    @Override
    public List<PlayerPermissionInfo> getValue()
    {
        return this.permissions;
    }
}
