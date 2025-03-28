package com.wintercogs.beyonddimensions.DataBase;

import com.wintercogs.beyonddimensions.DataBase.Storage.EnergyStorage;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Unit.PlayerNameHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class DimensionsNet extends WorldSavedData
{


    // 每个维度网络具有一个唯一标识符
    private int id;

    // 网络持有者
    private UUID owner;

    // 网络管理员 包含网络所有者
    private final Set<UUID> managers = new HashSet<>();

    // 与该网络绑定的玩家 包含网络管理者
    private final Set<UUID> players = new HashSet<>();

    // 网络存储空间
    private EnergyStorage energyStorage;
    // 非neoforge自带的存储系统 确保在任何调用之前检查null或者对应模组是否加载

    // 通用存储空间-测试 存储一切stack行为的资源
    private UnifiedStorage unifiedStorage;

    // 用于标记此网络是否为临时网络，如果是，则不执行倒计时或其他功能
    private final boolean temporary;

    private int currentTime = 600*20;
    private int holdTime = 600*20;

    public DimensionsNet()
    {
        super("BDNet_temporary");
        unifiedStorage = new UnifiedStorage(this);
        energyStorage = new EnergyStorage(this);
        //MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        this.temporary = true;
    }

    public DimensionsNet(String mapName)
    {
        super(mapName);
        unifiedStorage = new UnifiedStorage(this);
        energyStorage = new EnergyStorage(this);
        //MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        this.temporary = false;
    }


    // 构建最新的网络名称（适配1.12.2）
    public static String buildNewNetName(EntityPlayer player) {
        int netId;
        World world = player.getServer().getWorld(0); // 0是主世界维度ID

        for (netId = 0; netId < 10000; netId++) {
            // 1.12.2通过WorldSavedData的方式获取
            if (world.getPerWorldStorage().getOrLoadData(DimensionsNet.class, "BDNet_" + netId) == null) {
                break;
            }
        }
        return "BDNet_" + netId;
    }

    public static DimensionsNet getNetFromId(int id, World world) {
        if (id < 0) {
            return null;
        }
        // 通过世界存储获取数据
        return (DimensionsNet) world.getPerWorldStorage().getOrLoadData(DimensionsNet.class, "BDNet_" + id);
    }

    public static DimensionsNet getNetFromPlayer(EntityPlayer player) {
        World world = player.getEntityWorld().getMinecraftServer().getWorld(0); // 获取主世界

        for (int netId = 0; netId < 10000; netId++) {
            DimensionsNet net = (DimensionsNet) world.getPerWorldStorage().getOrLoadData(DimensionsNet.class, "BDNet_" + netId);
            if (net != null) {
                if (net.players.contains(player.getUniqueID())) { // 1.12.2使用getUniqueID()
                    return net;
                }
            } else {
                // 如果找不到数据则提前终止循环
                break;
            }
        }
        return null;
    }




    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        // 读取基础数据
        this.id = nbt.getInteger("Id");

        // 读取UUID需要转换字符串
        if (nbt.hasKey("Owner")) {
            this.owner = UUID.fromString(nbt.getString("Owner"));
        }

        // 读取存储
        this.energyStorage.deserializeNBT(nbt.getCompoundTag("EnergyStorage"));
        this.unifiedStorage.deserializeNBT(nbt.getCompoundTag("UnifiedStorage"));

        // 读取管理者列表
        this.managers.clear();
        NBTTagList managerList = nbt.getTagList("Managers", 8); // 8表示字符串类型
        for (int i = 0; i < managerList.tagCount(); i++) {
            managers.add(UUID.fromString(managerList.getStringTagAt(i)));
        }

        // 读取玩家列表
        this.players.clear();
        NBTTagList playerList = nbt.getTagList("Players", 8);
        for (int i = 0; i < playerList.tagCount(); i++) {
            players.add(UUID.fromString(playerList.getStringTagAt(i)));
        }

        // 读取倒计时
        this.currentTime = nbt.getInteger("currentTime");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        // 保存基础数据
        compound.setInteger("Id", this.id);

        // 保存UUID为字符串
        if (this.owner != null) {
            compound.setString("Owner", this.owner.toString());
        }

        // 保存存储
        compound.setTag("EnergyStorage", this.energyStorage.serializeNBT());
        compound.setTag("UnifiedStorage", this.unifiedStorage.serializeNBT());

        // 保存管理者列表
        NBTTagList managerList = new NBTTagList();
        for (UUID manager : managers) {
            managerList.appendTag(new NBTTagString(manager.toString()));
        }
        compound.setTag("Managers", managerList);

        // 保存玩家列表
        NBTTagList playerList = new NBTTagList();
        for (UUID player : players) {
            playerList.appendTag(new NBTTagString(player.toString()));
        }
        compound.setTag("Players", playerList);

        // 保存倒计时
        compound.setInteger("currentTime", this.currentTime);

        return compound;
    }


//    // 从硬盘加载数据
//    public static DimensionsNet load(CompoundTag tag)
//    {
//        DimensionsNet net = new DimensionsNet(false);
//
//        net.id = tag.getInt("Id");
//        UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
//        if (owner != null)
//        {
//            net.owner = owner;
//        }
//
//        net.unifiedStorage.deserializeNBT(tag.getCompound("UnifiedStorage"));
//        net.energyStorage.deserializeNBT(tag.getCompound("EnergyStorage"));
//
//        if (tag.contains("Managers"))
//        {
//            ListTag managerList = tag.getList("Managers",8);
//            managerList.forEach(manager -> net.managers.add(UUID.fromString(manager.getAsString())));
//        }
//
//        if (tag.contains("Players"))
//        {
//            ListTag playerList = tag.getList("Players", 8); // 8 表示 StringTag
//            playerList.forEach(player -> net.players.add(UUID.fromString(player.getAsString())));
//        }
//
//        // 读取倒计时
//        net.currentTime = tag.getInt("currentTime");
//
//        return net;
//    }
//
//    // 保存数据到硬盘
//    @Override
//    public CompoundTag save(CompoundTag tag)
//    {
//        // 保存 ID
//        tag.putInt("Id", this.id);
//        // 保存网络所有者 UUID
//        tag.putUUID("Owner", this.owner);
//
//        // 保存网络管理者
//        ListTag managerListTag = new ListTag();
//        for (UUID manager : managers)
//        {
//            managerListTag.add(StringTag.valueOf(manager.toString()));
//        }
//        tag.put("Managers",managerListTag);
//
//        // 保存绑定的玩家列表
//        ListTag playerListTag = new ListTag();
//        for (UUID player : players)
//        {
//            playerListTag.add(StringTag.valueOf(player.toString()));
//        }
//        tag.put("Players", playerListTag);
//
//        // 保存存储
//        tag.put("EnergyStorage",energyStorage.serializeNBT());
//        tag.put("UnifiedStorage",unifiedStorage.serializeNBT());
//
//        // 保存倒计时
//        tag.putInt("currentTime", this.currentTime);
//
//        return tag;
//    }


    // 功能函数

    // 获取维度网络ID
    public int getId()
    {
        return id;
    }

    public void setId(int Id)
    {
        this.id = Id;
        setDirty(true);
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
        setDirty(true);
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
        setDirty(true);
    }

    public void removeManager(UUID managerId)
    {
        if(managerId.equals(owner))
        {
            return;
        }
        managers.remove(managerId);
        setDirty(true);
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
        setDirty(true);
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
        setDirty(true);
    }

    public boolean isOwner(EntityPlayer player)
    {
        if(player.getUniqueID().equals(getOwner()))
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

    public boolean isManager(EntityPlayer player)
    {
        boolean flag = false;
        if(managers.contains(player.getUniqueID()))
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

    public HashMap<UUID,PlayerPermissionInfo> getPlayerPermissionInfoMap(World playerInfoProvider)
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

    public EnergyStorage getEnergyStorage()
    {
        return this.energyStorage;
    }

    // 统一存储空间
    public UnifiedStorage getUnifiedStorage()
    {
        return this.unifiedStorage;
    }

    // 用于定期生成破碎时空结晶
//    @SubscribeEvent
//    public void onServerTick(TickEvent.ServerTickEvent event)
//    {
//        // 不对临时网络执行倒计时
//        if(temporary)
//            return;
//
//        currentTime--;
//        setDirty();
//        if(currentTime <= 0)
//        {
//            ItemStack stack = new ItemStack(ModItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(),1);
//            IStackType stackType = new ItemStackType(stack);
//            this.unifiedStorage.insert(stackType,false);
//            currentTime = holdTime;
//        }
//
//    }
}

