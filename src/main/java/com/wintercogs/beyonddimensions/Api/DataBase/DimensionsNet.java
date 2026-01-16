package com.wintercogs.beyonddimensions.Api.DataBase;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.ServerConfig;
import com.wintercogs.beyonddimensions.Unit.PlayerNameHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * 此类即模组概念中的“维度网络”，并实际负责存储和持久化数据
 * <p>
 * 使用{@link DimensionsNet#createNewNetForPlayer(Player, long, int)}来创建一个持久化保存的维度网络
 * <p>
 */
public class DimensionsNet extends SavedData
{

    /**
     * 作为网络的唯一标识符，id从0开始，小于0的id均可以认为是无效网络
     * <p>
     * 被删除的网络均使用-99作为特殊标记
     */
    private int id;

    /**
     * deleted为真则表示网络被删除，被删除的网络仍可被{@link SavedData}的方法获得，但不应该被使用
     * <p>
     * 此数据会随着SavedData持久化保存
     */
    public boolean deleted = false;

    /**
     * 网络所有者
     */
    private UUID owner;

    /**
     * 网络管理员，包含所有者
     */
    private final Set<UUID> managers = new HashSet<>();

    /**
     * 网络成员，包含所有的管理员
     */
    private final Set<UUID> players = new HashSet<>();

    /**
     * 通用存储空间，存储任何实现了{@link com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey}的资源类型
     */
    private final @NotNull UnifiedStorage unifiedStorage;

    /**
     * 标记网络是否为一个临时网络，临时网络通常用于客户端菜单的同步中，作为资源容器使用
     * <p>
     * 临时网络不会执行生成破碎的时空结晶之类的操作
     */
    private final boolean temporary;

    /**
     * currentTime是流动的倒计时，用于生成破碎时空结晶，该数据持久化保存
     * <p>
     * holdTime是固定的时间间隔，用于确定多久生成一次时间间隔，每当currentTime归零，holdTime会为它赋值
     */
    private int currentTime = 0;

    /**
     * 构造函数
     * @param temporary 为真则说明是临时网络
     */
    public DimensionsNet(boolean temporary)
    {
        unifiedStorage = new UnifiedStorage(this, AbstractUnorderedStackHandler.UiTimestampPolicy.AUTO);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        this.temporary = temporary;
    }

    // 基本函数

    /**
     * 用于构造SavedData的工厂方法，一般来说，你不需要调用这个方法
     */
    public static DimensionsNet create()
    {
        return new DimensionsNet(false);
    }

    /**
     * 用于创建一个维度网络，仅在服务端调用
     * @param player 传入的玩家会作为网络所有者
     * @param defaultSlotCapability 新网络单个槽位可存储的容量
     * @param defaultSlotMaxSize 新网络所拥有的槽位数量
     * @return 返回新创建的维度网络，但如果传入的player加入了一个网络，只会返回其当前所在的网络
     */
    public static @Nullable DimensionsNet createNewNetForPlayer(Player player, long defaultSlotCapability, int defaultSlotMaxSize)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if(net != null) return net;

        MinecraftServer server = player.getServer();
        if(server != null)
        {
            String netId = DimensionsNet.buildNewNetName(server);
            String numId = netId.replace("BDNet_", "");
            DimensionsNet newNet = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), netId);
            newNet.setId(Integer.parseInt(numId));
            newNet.setOwner(player.getUUID());
            newNet.addManager(player.getUUID());
            newNet.addPlayer(player.getUUID());
            newNet.setDirty();
            newNet.unifiedStorage.setSlotCapacity(defaultSlotCapability);
            newNet.unifiedStorage.setSlotMaxSize(defaultSlotMaxSize);

            return newNet;
        }
        return null;
    }

    /**
     * 构建最新的，可用的网络名称，用于创建新网络时确定新网络的id，仅在服务端调用
     * @param dataProvider 用于获取SavedData
     * @return 最新可用的网络名称，内容为字符串："BDNet_<数字id>"
     */
    public static String buildNewNetName(@NotNull MinecraftServer dataProvider)
    {
        int netId;
        // 接下来按照"BDNet_" + netId从0查找网络，直到找到不存在的网络，此时netId为新网络id
        // 后续可以做一个废弃网络回收处理，但是暂时不着急
        for (netId = 0; netId < 10000; netId++)
        {
            if (dataProvider.overworld().getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), "BDNet_" + netId) == null)
            {
                break;
            }
        }
        return "BDNet_" + netId;
    }

    /**
     * 尝试从数字id获取一个维度网络，仅在服务端调用
     * @param id 数字id
     * @return 返回找到的网络，如果数字id对应的网络不存在或者不合法(例如被删除)，则直接返回null
     */
    public static @Nullable DimensionsNet getNetFromId(int id)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(server == null) return null;
        if(id<0) return null;

        DimensionsNet net = server.overworld().getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), "BDNet_" + id);
        if(net != null && !net.deleted)
        {
            return net;
        }
        return null;
    }

    /**
     * 尝试从玩家获取维度网络，仅在服务端调用
     * @param player 玩家
     * @return 返回玩家所在的维度网络，如果不存在，则返回null
     */
    public static @Nullable DimensionsNet getNetFromPlayer(Player player)
    {
        MinecraftServer server = player.getServer();
        if(server == null) return null;

        int netId;
        for (netId = 0; netId < 10000; netId++)
        {
            DimensionsNet net = server.overworld().getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), "BDNet_" + netId);
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

    /**
     * 从硬盘反序列化维度网络，用于SavedData的工厂方法
     */
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
                net.unifiedStorage.insert(EnergyStackKey.INSTANCE, energyTag.getLong("Energy"),false);
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

    /**
     * 把维度网络序列化，以保存到硬盘
     */
    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registryAccess)
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

    /**
     * 获取网络id
     */
    public int getId()
    {
        return id;
    }

    /**
     * 设置网络id
     */
    public void setId(int Id)
    {
        this.id = Id;
        setDirty();
    }

    /**
     * 获取网络所有者的uuid
     */
    public UUID getOwner()
    {
        return owner;
    }

    /**
     * 设置新的网络所有者
     * <p>
     * 注意，这不会把原所有者从网络中删除，他会成为网络管理员
     */
    public void setOwner(UUID owner)
    {
        this.owner = owner;
        addManager(owner);
        setDirty();
    }

    /**
     * 获取包含所有管理员uuid的集合
     */
    public Set<UUID> getManagers()
    {
        return managers;
    }

    /**
     * 添加一个网络管理员
     * @param managerId 新增管理员的uuid
     */
    public void addManager(UUID managerId)
    {
        managers.add(managerId);
        addPlayer(managerId);
        setDirty();
    }

    /**
     * 移除一个网络管理员，该管理员将会降级为成员
     * <p>
     * 不能直接移除当前所有者
     */
    public void removeManager(UUID managerId)
    {
        if(managerId.equals(owner))
        {
            return;
        }
        managers.remove(managerId);
        setDirty();
    }

    /**
     * 获取当前网络所有的玩家集合
     */
    public Set<UUID> getPlayers()
    {
        return players;
    }

    /**
     * 添加一个网络成员
     */
    public void addPlayer(UUID playerId)
    {
        players.add(playerId);
        setDirty();
    }

    /**
     * 从网络移除一个玩家，你不能直接移除当前所有者
     * <p>
     * 但是你可以直接移除任何其他成员
     */
    public void removePlayer(UUID playerId)
    {
        if(playerId == owner)
        {
            return;
        }
        players.remove(playerId);
        managers.remove(playerId);
        setDirty();
    }

    /**
     * 传入的玩家是否为所有者
     * @param player 玩家
     * @return 是所有者则返回真
     */
    public boolean isOwner(Player player)
    {
        return player.getUUID().equals(getOwner());
    }

    /**
     * 传入的玩家uuid是否为所有者
     * @param playerId 玩家的uuid
     * @return 是所有者则返回真
     */
    public boolean isOwner(UUID playerId)
    {
        return playerId.equals(getOwner());
    }

    /**
     * 传入的玩家是否为管理员
     */
    public boolean isManager(Player player)
    {
        return managers.contains(player.getUUID());
    }

    /**
     * 传入的玩家uuid是否为管理员
     */
    public boolean isManager(UUID playerId)
    {
        return managers.contains(playerId);
    }

    /**
     * 合并另一个网络，其所有资源，玩家均被合并，但其绑定的方块会自动解绑（通过标记另一个网络为被删除实现）
     * <p>
     * 仅在服务端使用
     * @param otherNet 被合并的网络
     */
    public void mergeOtherNet(DimensionsNet otherNet)
    {
        // 合并玩家和管理员
        for(Map.Entry<UUID,PlayerPermissionInfo> entry: otherNet.getPlayerPermissionInfoMap(ServerLifecycleHooks.getCurrentServer()).entrySet())
        {
            if(entry.getValue().level() == NetPermissionlevel.Owner ||entry.getValue().level() == NetPermissionlevel.Manager)
                addManager(entry.getKey());
            else if(entry.getValue().level() == NetPermissionlevel.Member)
                addPlayer(entry.getKey());
        }
        // 合并统一存储系统
        for(KeyAmount stack : otherNet.getUnifiedStorage().getStorage())
        {
            unifiedStorage.insert(stack.key(),stack.amount(),false);
        }

        // 销毁另一个网络
        otherNet.destroySelf();
    }

    /**
     * 销毁当前网络
     */
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


    /**
     * 获取一份当前网络所有玩家的UUID以及其对应的最高权限等级的映射
     */
    public HashMap<UUID,PlayerPermissionInfo> getPlayerPermissionInfoMap(@NotNull MinecraftServer dataProvider)
    {

        HashMap<UUID,PlayerPermissionInfo> infoMap = new HashMap<>();
        for(UUID playerId :players)
        {
            if(isOwner(playerId))
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId,dataProvider),NetPermissionlevel.Owner));
            }
            else if(isManager(playerId))
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId,dataProvider),NetPermissionlevel.Manager));
            }
            else
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId,dataProvider),NetPermissionlevel.Member));
            }
        }
        return infoMap;
    }

    /**
     * 获取当前网络所携带的统一存储空间，统一存储空间是网络存储资源的地方
     * @return 当前网络的统一存储空间
     */
    public @NotNull UnifiedStorage getUnifiedStorage()
    {
        return this.unifiedStorage;
    }

    /**
     * 用于执行定期操作，目前仅用于生成破碎的时空结晶
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event)
    {
        // 不对临时网络执行倒计时
        if(temporary || ServerConfig.CRYSTAL_GENERATE_TIME <= 0)
            return;

        currentTime++;
        setDirty();
        if(currentTime >= ServerConfig.CRYSTAL_GENERATE_TIME * 20)
        {
            ItemStack stack = new ItemStack(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(),1);
            this.unifiedStorage.insert(new ItemStackKey(stack),stack.getCount(),false);
            currentTime = 0;
        }

    }
}

