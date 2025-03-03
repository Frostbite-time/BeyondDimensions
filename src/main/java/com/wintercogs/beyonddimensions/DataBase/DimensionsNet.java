package com.wintercogs.beyonddimensions.DataBase;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Storage.*;
import com.wintercogs.beyonddimensions.Unit.PlayerNameHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


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

    // 网络存储空间
    private ItemStorage itemStorage;
    private FluidStorage fluidStorage;
    private EnergyStorage energyStorage;
    // 非neoforge自带的存储系统 确保在任何调用之前检查null或者对应模组是否加载
    private ChemicalStorage chemicalStorage;

    // 通用存储空间-测试 存储一切stack行为的资源
    private UnifiedStorage unifiedStorage;


    public DimensionsNet()
    {
        unifiedStorage = new UnifiedStorage(this);

        itemStorage = new ItemStorage(this);
        fluidStorage = new FluidStorage(this);
        energyStorage = new EnergyStorage(this);

        if(BeyondDimensions.MekLoaded)
        {
            chemicalStorage = new ChemicalStorage(this);
        }
        else
        {
            chemicalStorage = null;
        }
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

    public static DimensionsNet getNetFromId(int id, Level storageProvider)
    {
        if(id<0)
        {
            return null;
        }
        DimensionsNet net = storageProvider.getServer().getLevel(Level.OVERWORLD).getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), "BDNet_" + id);
        if(net !=null)
        {
            return net;
        }
        return null;
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
        net.fluidStorage.deserializeNBT(registryAccess,tag.getCompound("FluidStorage"));
        net.energyStorage.deserializeNBT(registryAccess,tag.getCompound("EnergyStorage"));
        net.unifiedStorage.deserializeNBT(registryAccess,tag.getCompound("UnifiedStorage"));
        if(net.chemicalStorage != null)
        {
            net.chemicalStorage.deserializeNBT(registryAccess,tag.getCompound("ChemicalStorage"));
        }

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
        tag.put("FluidStorage",fluidStorage.serializeNBT(registryAccess));
        tag.put("EnergyStorage",energyStorage.serializeNBT(registryAccess));
        tag.put("UnifiedStorage",unifiedStorage.serializeNBT(registryAccess));
        if(chemicalStorage != null)
        {
            tag.put("ChemicalStorage",chemicalStorage.serializeNBT(registryAccess));
        }

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
        if(managerId.equals(owner))
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

    public boolean isOwner(Player player)
    {
        if(player.getUUID().equals(getOwner()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean isOwner(UUID playerId)
    {
        if(playerId.equals(getOwner()))
        {
            return true;
        }
        else
        {
            return false;
        }
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

    public boolean isManager(UUID playerId)
    {
        boolean flag = false;
        if(managers.contains(playerId))
        {
            flag = true;
        }
        return flag;
    }

    public HashMap<UUID,PlayerPermissionInfo> getPlayerPermissionInfoMap(Level playerInfoProvider)
    {

        HashMap<UUID,PlayerPermissionInfo> infoMap = new HashMap<>();
        for(UUID playerId :players)
        {
            if(isOwner(playerId))
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId,playerInfoProvider),NetPermissionlevel.Owner));
            }
            else if(isManager(playerId))
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId,playerInfoProvider),NetPermissionlevel.Manager));
            }
            else
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId,playerInfoProvider),NetPermissionlevel.Member));
            }
        }
        return infoMap;
    }

    //物品存储
    public ItemStorage getItemStorage()
    {
        return this.itemStorage;
    }

    public FluidStorage getFluidStorage()
    {
        return this.fluidStorage;
    }

    public EnergyStorage getEnergyStorage()
    {
        return this.energyStorage;
    }

    public ChemicalStorage getChemicalStorage()
    {
        return this.chemicalStorage;
    }

    // 统一存储空间
    public UnifiedStorage getUnifiedStorage()
    {
        return this.unifiedStorage;
    }

}

