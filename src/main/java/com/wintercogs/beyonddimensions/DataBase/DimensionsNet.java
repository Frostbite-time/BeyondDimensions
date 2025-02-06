package com.wintercogs.beyonddimensions.DataBase;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.*;


public class DimensionsNet extends SavedData
{

    private static final Logger LOGGER = LogUtils.getLogger();

    // 每个维度网络具有一个唯一标识符
    private int id;

    // 网络持有者
    private UUID owner;

    // 网络管理员 包含网络所有者
    private final Set<UUID> managers = new HashSet<>();

    // 与该网络绑定的玩家 包含网络管理者
    private final Set<UUID> players = new HashSet<>();

    // 网络的物品存储空间
    private DimensionsItemStorage itemStorage;






    // 构造函数，传入网络管理者的UUID
    public DimensionsNet()
    {
        itemStorage = new DimensionsItemStorage(this);

    }

    // 基本函数

    // Create函数
    public static DimensionsNet create()
    {
        return new DimensionsNet();
    }

    // 构建最新的网络名称
    public static String buildNewNetName(Player player)
    {
        int netId;
        // 接下来按照"BDNet_" + netId从0查找网络，直到找到不存在的网络，此时netId为新网络id
        for (netId = 0; netId < 10000; netId++)
        {
            if (player.getServer().getLevel(Level.OVERWORLD).getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), "BDNet_" + netId) == null)
            {
                break;
            }
        }
        return "BDNet_" + netId;
    }


    public static DimensionsNet getNetFromPlayer(Player player)
    {
        int netId;
        for (netId = 0; netId < 10000; netId++)
        {
            DimensionsNet net = player.getServer().getLevel(Level.OVERWORLD).getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), "BDNet_" + netId);
            if (net != null)
            {
                if(net.players.contains(player.getUUID()))
                {
                    return net;
                }
            }
            else
            {
                return null;
            }
        }
        return null;
    }

    // 从硬盘加载数据
    public static DimensionsNet load(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        DimensionsNet net = new DimensionsNet();

        net.id = tag.getInt("Id");
        UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (owner != null)
        {
            net.owner = owner;
        }


        net.itemStorage.deserializeNBT(registryAccess, tag.getCompound("ItemStorage"));

        if (tag.contains("Managers"))
        {
            ListTag managerList = tag.getList("Managers",8);
            managerList.forEach(manager -> net.managers.add(UUID.fromString(manager.getAsString())));
        }

        if (tag.contains("Players"))
        {
            ListTag playerList = tag.getList("Players", 8); // 8 表示 StringTag
            playerList.forEach(player -> net.players.add(UUID.fromString(player.getAsString())));
        }

        return net;
    }

    // 保存数据到硬盘
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        // 保存 ID
        tag.putInt("Id", this.id);
        // 保存网络所有者 UUID
        tag.putUUID("Owner", this.owner);

        // 保存网络管理者
        ListTag managerListTag = new ListTag();
        for (UUID manager : managers)
        {
            managerListTag.add(StringTag.valueOf(manager.toString()));
        }
        tag.put("Managers",managerListTag);

        // 保存绑定的玩家列表
        ListTag playerListTag = new ListTag();
        for (UUID player : players)
        {
            playerListTag.add(StringTag.valueOf(player.toString()));
        }
        tag.put("Players", playerListTag);

        // 保存物品存储
        tag.put("ItemStorage", itemStorage.serializeNBT(registryAccess));

        return tag;
    }


    // 功能函数

    // 获取维度网络ID
    public int getId()
    {
        return id;
    }

    public void setId(int Id)
    {
        this.id = Id;
        setDirty();
    }

    // 获取网络拥有者ID
    public UUID getOwner()
    {
        return owner;
    }

    // 设置网络拥有者ID
    public void setOwner(UUID owner)
    {
        this.owner = owner;
        setDirty();
    }

    // 获取所有管理员
    public Set<UUID> getManagers()
    {
        return managers;
    }

    // 添加管理员
    public void addManager(UUID managerId)
    {
        managers.add(managerId);
        setDirty();
    }

    public void removeManager(UUID managerId)
    {
        if(managers.contains(owner))
        {
            return;
        }
        managers.remove(managerId);
        setDirty();
    }

    // 获取所有绑定的玩家
    public Set<UUID> getPlayers()
    {
        return players;
    }

    // 添加玩家到网络
    public void addPlayer(UUID playerId)
    {
        players.add(playerId);
        setDirty();
    }

    // 移除玩家
    public void removePlayer(UUID playerId)
    {
        if(playerId == owner)
        {
            return;
        }
        players.remove(playerId);
        if(managers.contains(playerId))
        {
            managers.remove(playerId);
        }
        setDirty();
    }

    public boolean isManager(Player player)
    {
        boolean flag = false;
        if(managers.contains(player.getUUID()))
        {
            flag = true;
        }
        return flag;
    }

    //物品存储
    public DimensionsItemStorage getItemStorage()
    {
        return this.itemStorage;
    }

    public void addItem(ItemStack itemStack, long count)
    {
        this.itemStorage.addItem(itemStack, count);
        setDirty();
    }

    public ItemStack removeItem(ItemStack itemStack, long count)
    {
        setDirty();
        return this.itemStorage.removeItem(itemStack, count);
    }



}

