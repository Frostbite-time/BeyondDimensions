package com.wintercogs.beyonddimensions.DataBase;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Unit.PlayerNameHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.*;


public class DimensionsNet extends SavedData
{

    private static final Logger LOGGER = LogUtils.getLogger();

    // 每个维度网络具有一个唯一标识符
    // 被删除的网络id会被标记为-99
    private int id;

    public boolean deleted = false; // 正常被初始化的为false，该数据持久化，用以记录被删除的网络
                                    // 直到功能测试稳定，被删除的网络可以重新分配给其他玩家

    // 网络持有者
    private UUID owner;

    // 网络管理员 包含网络所有者
    private final Set<UUID> managers = new HashSet<>();

    // 与该网络绑定的玩家 包含网络管理者
    private final Set<UUID> players = new HashSet<>();

    // 通用存储空间 存储一切stack行为的资源
    private UnifiedStorage unifiedStorage;

    // 用于标记此网络是否为临时网络，如果是，则不执行倒计时或其他功能
    private final boolean temporary;

    private int currentTime = 600*20;
    private int holdTime = 600*20;

    public DimensionsNet(boolean temporary)
    {
        unifiedStorage = new UnifiedStorage(this);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        this.temporary = temporary;
    }

    // 基本函数

    // Create函数
    public static DimensionsNet create()
    {
        return new DimensionsNet(false);
    }

    // 构建最新的可用网络名称
    public static String buildNewNetName(Player player)
    {
        int netId;
        // 接下来按照"BDNet_" + netId从0查找网络，直到找到不存在的网络，此时netId为新网络id
        // 后续可以做一个废弃网络回收处理，但是暂时不着急
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
        if(net !=null && !net.deleted)
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
            if (net != null && !net.deleted)
            {
                if(net.players.contains(player.getUUID()))
                {
                    return net;
                }
            }
            else
            {
                continue; // 给予10000次查找机会，防止乱删乱改导致id轮空
            }
        }
        return null;
    }

    // 从硬盘加载数据
    public static DimensionsNet load(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        DimensionsNet net = new DimensionsNet(false);

        net.id = tag.getInt("Id");
        UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (owner != null)
        {
            net.owner = owner;
        }

        net.unifiedStorage.deserializeNBT(registryAccess,tag.getCompound("UnifiedStorage"));
        // 旧数据兼容
        if(tag.contains("EnergyStorage"))
        {
            CompoundTag energyTag = tag.getCompound("EnergyStorage");
            if (energyTag.contains("Energy"))
            {
                net.unifiedStorage.insert(new EnergyStackType(energyTag.getLong("Energy")),false);
            }
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

        // 读取倒计时
        net.currentTime = tag.getInt("currentTime");

        if(tag.contains("Deleted"))
            net.deleted = tag.getBoolean("Deleted");

        return net;
    }

    // 保存数据到硬盘
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        // 保存 ID
        tag.putInt("Id", this.id);
        // 保存网络所有者 UUID
        if(this.owner != null)
            tag.putUUID("Owner", this.owner);

        if(!tag.contains("OldDataTag"))
        {
            tag.putBoolean("OldDataTag", true);
        }

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

        // 保存存储
        tag.put("UnifiedStorage",unifiedStorage.serializeNBT(registryAccess));

        // 保存倒计时
        tag.putInt("currentTime", this.currentTime);

        // 保存删除状态
        tag.putBoolean("Deleted", this.deleted);

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
        addManager(owner);
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
        addPlayer(managerId);
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

    public void mergeOtherNet(DimensionsNet otherNet)
    {
        Level provider = ServerLifecycleHooks.getCurrentServer().overworld();
        // 合并玩家和管理员
        for(Map.Entry<UUID,PlayerPermissionInfo> entry: otherNet.getPlayerPermissionInfoMap(provider).entrySet())
        {
            if(entry.getValue().level() == NetPermissionlevel.Owner ||entry.getValue().level() == NetPermissionlevel.Manager)
                addManager(entry.getKey());
            else if(entry.getValue().level() == NetPermissionlevel.Member)
                addPlayer(entry.getKey());
        }
        // 合并统一存储系统
        for(IStackType stack : otherNet.getUnifiedStorage().getStorage())
        {
            unifiedStorage.insert(stack,false);
        }

        // 销毁另一个网络
        otherNet.destroySelf();
    }

    public void destroySelf()
    {
        // 这里有一些问题。即我们实际上无法删除已经存在的SaveData。
        // 所以我们要做的是巧妙地将此SaveData有关数据指向移除。
        // 然后将所有对应的存储容量设置为0
        this.owner = null;
        this.managers.clear();
        this.players.clear();
        this.id = -99; // 用-99作为被删除的特殊标记
        this.unifiedStorage.clearStorage();
        this.deleted = true;
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

    // 统一存储空间
    public UnifiedStorage getUnifiedStorage()
    {
        return this.unifiedStorage;
    }

    // 用于定期生成破碎时空结晶
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event)
    {
        // 不对临时网络执行倒计时
        if(temporary)
            return;

        currentTime--;
        setDirty();
        if(currentTime <= 0)
        {
            ItemStack stack = new ItemStack(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(),1);
            IStackType stackType = new ItemStackType(stack);
            this.unifiedStorage.insert(stackType,false);
            currentTime = holdTime;
        }

    }
}

