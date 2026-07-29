package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.MobStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.PigStackKey;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import com.wintercogs.beyonddimensions.util.PlayerNameHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    static final String NET_DATA_PREFIX = "BDNet_";
    public static final int NO_PRIMARY_NET_ID = -1;
    public static final int MAX_NETWORK_NAME_LENGTH = 48;
    private static final String CUSTOM_NAME_TAG = "custom_name";

    /**
     * 作为网络的唯一标识符，id从0开始，小于0的id均可以认为是无效网络
     * <p>
     * 被删除的网络均使用-99作为特殊标记
     */
    private int id;

    /**
     * 玩家自定义网络名。空字符串表示未命名，展示时回退到本地化默认名。
     */
    private String customName = "";

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
     * 通用存储空间，存储任何实现了{@link IStackKey}的资源类型
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
     * 各类动物和村民的繁殖进度。每种实体独立计时，约5分钟完成一轮繁殖。
     */
    private final Map<ResourceLocation, Integer> mobBreedingTimes = new HashMap<>();
    private final Map<EntityType<?>, ItemStackKey> animalFoodKeys = new HashMap<>();
    private static final int MOB_BREEDING_INTERVAL = 6000;
    private static final int RANCH_TICK_INTERVAL = 20;
    private static final int VILLAGER_FOOD_POINTS = 12;
    private static final Map<net.minecraft.world.item.Item, Integer> VILLAGER_FOODS = Map.of(
            Items.BREAD, 4,
            Items.CARROT, 1,
            Items.POTATO, 1,
            Items.BEETROOT, 1
    );

    /**
     * 构造函数
     *
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
     *
     * @param player                传入的玩家会作为网络所有者
     * @param defaultSlotCapability 新网络单个槽位可存储的容量
     * @param defaultSlotMaxSize    新网络所拥有的槽位数量
     * @return 返回新创建的维度网络，但如果传入的player加入了一个网络，只会返回其当前所在的网络
     */
    public static @Nullable DimensionsNet createNewNetForPlayer(Player player, long defaultSlotCapability, int defaultSlotMaxSize)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null) return net;

        MinecraftServer server = player.getServer();
        if (server != null)
        {
            int allocatedNetId = NetRegistryIndex.get(server).allocateNetId(server);
            String netDataName = DimensionsNet.buildNetDataName(allocatedNetId);
            DimensionsNet newNet = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), netDataName);
            newNet.setId(allocatedNetId);
            NetRegistryIndex.get(server).registerNet(server, allocatedNetId);
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
     * 构建最新的，可用的网络名称，仅在服务端调用。
     * <p>
     * 该方法会通过网络注册表分配一个全新的、单调递增的网络 ID，并基于它构建数据名。
     * 该操作不会自动把网络登记为有效网络。
     *
     * @param dataProvider 用于获取SavedData
     * @return 最新可用的网络名称，内容为字符串："BDNet_<数字id>"
     */
    public static String buildNewNetName(@NotNull MinecraftServer dataProvider)
    {
        return buildNetDataName(NetRegistryIndex.get(dataProvider).allocateNetId(dataProvider));
    }

    /**
     * 根据网络 ID 构建其固定的数据存档名。
     *
     * @param netId 网络 ID
     * @return 对应的 SavedData 名称
     */
    public static String buildNetDataName(int netId)
    {
        return NET_DATA_PREFIX + netId;
    }

    /**
     * 尝试从数字id获取一个维度网络，仅在服务端调用
     *
     * @param id 数字id
     * @return 返回找到的网络，如果数字id对应的网络不存在或者不合法(例如被删除)，则直接返回null
     */
    public static @Nullable DimensionsNet getNetFromId(int id)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return getNetFromId(server, id);
    }

    static @Nullable DimensionsNet getNetFromId(@NotNull MinecraftServer server, int id)
    {
        if (id < 0) return null;

        DimensionsNet net = server.overworld().getDataStorage().get(new SavedData.Factory<>(DimensionsNet::create, DimensionsNet::load), buildNetDataName(id));
        if (net != null && !net.deleted)
        {
            return net;
        }
        return null;
    }

    /**
     * 尝试从玩家获取维度网络，仅在服务端调用
     *
     * @param player 玩家
     * @return 返回玩家所在的维度网络，如果不存在，则返回null
     */
    @Deprecated(forRemoval = false)
    public static @Nullable DimensionsNet getNetFromPlayer(Player player)
    {
        return getPrimaryNetFromPlayer(player);
    }

    /**
     * 尝试从玩家获取主维度网络，仅在服务端调用。
     * <p>
     * 这是新的主网络语义接口：玩家即使仍然属于其他网络，只要主网络被显式清空，
     * 这里依然会返回 {@code null}。
     *
     * @param player 玩家
     * @return 玩家当前的主网络；如果没有主网络，则返回 {@code null}
     */
    public static @Nullable DimensionsNet getPrimaryNetFromPlayer(Player player)
    {
        MinecraftServer server = player.getServer();
        if (server == null) return null;

        PlayerNetIndex index = PlayerNetIndex.get(server);
        int primaryNetId = index.getPrimaryNetId(player.getUUID());
        if (primaryNetId == PlayerNetIndex.NO_PRIMARY_NET)
        {
            return null;
        }

        return getNetFromId(server, primaryNetId);
    }

    /**
     * 获取玩家所属的全部有效网络，仅在服务端调用。
     * <p>
     * 该接口返回运行时维护的全部成员网络，并会自动过滤已删除或无效的网络。
     * 返回结果不受主网络是否为空影响。
     *
     * @param player 玩家
     * @return 玩家所属的全部有效网络；如果没有任何成员网络，则返回空列表
     */
    public static @NotNull List<DimensionsNet> getAllNetFromPlayer(Player player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return List.of();
        }

        PlayerNetIndex index = PlayerNetIndex.get(server);
        List<Integer> netIds = index.getAllNetIds(player.getUUID());
        if (netIds.isEmpty())
        {
            return List.of();
        }

        List<DimensionsNet> nets = new ArrayList<>(netIds.size());
        for (int netId : netIds)
        {
            DimensionsNet net = getNetFromId(server, netId);
            if (net != null)
            {
                nets.add(net);
            }
        }
        return nets;
    }

    /**
     * 判断玩家是否属于任意维度网络，仅在服务端调用。
     * <p>
     * 该方法检查的是全部成员关系，而不是主网络状态。
     *
     * @param player 玩家
     * @return 玩家只要仍属于任意网络，就返回 {@code true}
     */
    public static boolean hasAnyNet(Player player)
    {
        MinecraftServer server = player.getServer();
        return server != null && PlayerNetIndex.get(server).hasAnyMembership(player.getUUID());
    }

    /**
     * 判断玩家当前是否拥有主网络，仅在服务端调用。
     *
     * @param player 玩家
     * @return 玩家存在主网络时返回 {@code true}
     */
    public static boolean hasPrimaryNet(Player player)
    {
        return getPrimaryNetFromPlayer(player) != null;
    }

    /**
     * 设置玩家的主网络，仅在服务端调用。
     * <p>
     * 传入 {@code null} 时会把玩家切换到空主网络状态，但不会移除其已有的成员网络。
     * 传入非空网络时，要求玩家已经属于该网络。
     *
     * @param player 玩家
     * @param net    目标主网络；传入 {@code null} 表示清空主网络
     * @return 主网络状态实际发生变化时返回 {@code true}
     */
    public static boolean setPrimaryNetForPlayer(Player player, @Nullable DimensionsNet net)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return false;
        }

        PlayerNetIndex index = PlayerNetIndex.get(server);
        return net == null
                ? index.setPrimary(player.getUUID(), PlayerNetIndex.NO_PRIMARY_NET)
                : index.setPrimary(player.getUUID(), net.getId());
    }

    /**
     * 清空玩家的主网络，仅在服务端调用。
     * <p>
     * 该操作不会移除玩家已有的成员关系，因此玩家仍然可能通过
     * {@link #getAllNetFromPlayer(Player)} 查到其他网络。
     *
     * @param player 玩家
     */
    public static void clearPrimaryNetForPlayer(Player player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }

        PlayerNetIndex.get(server).clearPrimary(player.getUUID());
    }

    /**
     * 从硬盘反序列化维度网络，用于SavedData的工厂方法
     */
    public static DimensionsNet load(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        DimensionsNet net = new DimensionsNet(false);

        net.id = tag.getInt("Id");
        if (tag.contains(CUSTOM_NAME_TAG))
            net.customName = sanitizeCustomName(tag.getString(CUSTOM_NAME_TAG));

        UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (owner != null)
        {
            net.owner = owner;
        }

        net.unifiedStorage.deserializeNBT(registryAccess, tag.getCompound("UnifiedStorage"));
        // 把旧版养猪场的猪数量迁移到通用实体存储。
        long legacyPigs = net.unifiedStorage.getStackByKey(PigStackKey.INSTANCE).amount();
        if (legacyPigs > 0)
        {
            net.unifiedStorage.extract(PigStackKey.INSTANCE, legacyPigs, false, false);
            net.unifiedStorage.insert(new MobStackKey(EntityType.PIG), legacyPigs, false);
        }
        // 旧数据兼容
        if (tag.contains("EnergyStorage"))
        {
            CompoundTag energyTag = tag.getCompound("EnergyStorage");
            if (energyTag.contains("Energy"))
            {
                net.unifiedStorage.insert(EnergyStackKey.INSTANCE, energyTag.getLong("Energy"), false);
            }
        }

        if (tag.contains("Managers"))
        {
            ListTag managerList = tag.getList("Managers", 8);
            managerList.forEach(manager -> net.managers.add(UUID.fromString(manager.getAsString())));
        }

        if (tag.contains("Players"))
        {
            ListTag playerList = tag.getList("Players", 8); // 8 表示 StringTag
            playerList.forEach(player -> net.players.add(UUID.fromString(player.getAsString())));
        }

        // 读取倒计时
        net.currentTime = tag.getInt("currentTime");
        CompoundTag breedingTimesTag = tag.getCompound("mobBreedingTimes");
        for (String key : breedingTimesTag.getAllKeys())
        {
            ResourceLocation typeId = ResourceLocation.tryParse(key);
            if (typeId != null)
                net.mobBreedingTimes.put(typeId, Math.max(0, breedingTimesTag.getInt(key)));
        }
        if (tag.contains("pigBreedingTime"))
            net.mobBreedingTimes.put(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PIG),
                    Math.max(0, tag.getInt("pigBreedingTime")));

        if (tag.contains("Deleted"))
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
        if (!this.customName.isEmpty())
            tag.putString(CUSTOM_NAME_TAG, this.customName);

        // 保存网络所有者 UUID
        if (this.owner != null)
            tag.putUUID("Owner", this.owner);

        if (!tag.contains("OldDataTag"))
        {
            tag.putBoolean("OldDataTag", true);
        }

        // 保存网络管理者
        ListTag managerListTag = new ListTag();
        for (UUID manager : managers)
        {
            managerListTag.add(StringTag.valueOf(manager.toString()));
        }
        tag.put("Managers", managerListTag);

        // 保存绑定的玩家列表
        ListTag playerListTag = new ListTag();
        for (UUID player : players)
        {
            playerListTag.add(StringTag.valueOf(player.toString()));
        }
        tag.put("Players", playerListTag);

        // 保存存储
        tag.put("UnifiedStorage", unifiedStorage.serializeNBT(registryAccess));

        // 保存倒计时
        tag.putInt("currentTime", this.currentTime);
        CompoundTag breedingTimesTag = new CompoundTag();
        mobBreedingTimes.forEach((typeId, time) -> breedingTimesTag.putInt(typeId.toString(), time));
        tag.put("mobBreedingTimes", breedingTimesTag);

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
     * 获取用于展示的网络名。自定义名为空时，返回本地化默认名。
     */
    public Component getNetworkName()
    {
        return getNetworkName(this.id, this.customName);
    }

    /**
     * 供客户端快照等没有 DimensionsNet 实例的场景复用同一命名规则。
     */
    public static Component getNetworkName(int netId, @Nullable String customName)
    {
        String sanitizedName = sanitizeCustomName(customName);
        if (!sanitizedName.isEmpty())
            return Component.literal(sanitizedName);

        return Component.translatable("menu.text.beyonddimensions.net.default_name", netId);
    }

    public String getCustomName()
    {
        return customName;
    }

    public boolean hasCustomName()
    {
        return !customName.isEmpty();
    }

    public void setCustomName(@Nullable String customName)
    {
        String sanitizedName = sanitizeCustomName(customName);
        if (Objects.equals(this.customName, sanitizedName))
            return;

        this.customName = sanitizedName;
        setDirty();
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
     *
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
        if (managerId.equals(owner))
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
        if (players.add(playerId))
        {
            syncPlayerMembership(playerId, true);
            setDirty();
        }
    }

    /**
     * 从网络移除一个玩家，你不能直接移除当前所有者
     * <p>
     * 但是你可以直接移除任何其他成员
     */
    public void removePlayer(UUID playerId)
    {
        if (playerId.equals(owner))
        {
            return;
        }
        if (players.remove(playerId))
        {
            managers.remove(playerId);
            syncPlayerRemoval(playerId);
            setDirty();
        }
    }

    /**
     * 传入的玩家是否为所有者
     *
     * @param player 玩家
     * @return 是所有者则返回真
     */
    public boolean isOwner(Player player)
    {
        return player.getUUID().equals(getOwner());
    }

    /**
     * 传入的玩家uuid是否为所有者
     *
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
     *
     * @param otherNet 被合并的网络
     */
    public void mergeOtherNet(DimensionsNet otherNet)
    {
        // 合并玩家和管理员
        for (Map.Entry<UUID, PlayerPermissionInfo> entry : otherNet.getPlayerPermissionInfoMap(ServerLifecycleHooks.getCurrentServer()).entrySet())
        {
            if (entry.getValue().level() == NetPermissionlevel.Owner || entry.getValue().level() == NetPermissionlevel.Manager)
                addManager(entry.getKey());
            else if (entry.getValue().level() == NetPermissionlevel.Member)
                addPlayer(entry.getKey());
        }
        // 合并统一存储系统
        for (KeyAmount stack : otherNet.getUnifiedStorage().getStorage())
        {
            unifiedStorage.insert(stack.key(), stack.amount(), false);
        }

        // 销毁另一个网络
        otherNet.destroySelf();
    }

    /**
     * 销毁当前网络
     */
    public void destroySelf()
    {
        int previousNetId = this.id;
        List<UUID> playerIds = new ArrayList<>(this.players);
        for (UUID playerId : playerIds)
        {
            syncPlayerRemoval(playerId, previousNetId);
        }

        // 这里有一些问题。即我们实际上无法删除已经存在的SaveData。
        // 所以我们要做的是巧妙地将此SaveData有关数据指向移除。
        // 然后将所有对应的存储容量设置为0
        this.owner = null;
        this.managers.clear();
        this.players.clear();
        this.id = -99; // 用-99作为被删除的特殊标记
        this.unifiedStorage.clearStorage();
        this.deleted = true;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            NetRegistryIndex.get(server).unregisterNet(server, previousNetId);
        }
        setDirty();
    }


    /**
     * 获取一份当前网络所有玩家的UUID以及其对应的最高权限等级的映射
     */
    public HashMap<UUID, PlayerPermissionInfo> getPlayerPermissionInfoMap(@NotNull MinecraftServer dataProvider)
    {

        HashMap<UUID, PlayerPermissionInfo> infoMap = new HashMap<>();
        for (UUID playerId : players)
        {
            if (isOwner(playerId))
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId, dataProvider), NetPermissionlevel.Owner));
            }
            else if (isManager(playerId))
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId, dataProvider), NetPermissionlevel.Manager));
            }
            else
            {
                infoMap.put(playerId, new PlayerPermissionInfo(PlayerNameHelper.getPlayerNameByUUID(playerId, dataProvider), NetPermissionlevel.Member));
            }
        }
        return infoMap;
    }

    /**
     * 获取当前网络所携带的统一存储空间，统一存储空间是网络存储资源的地方
     *
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
        if (temporary) return;

        tickRanchBreeding(event.getServer().overworld());

        if (ServerConfigRuntime.crystalGenerateTime <= 0) return;

        currentTime++;
        setDirty();
        if (currentTime >= ServerConfigRuntime.crystalGenerateTime * 20)
        {
            ItemStack stack = new ItemStack(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(), 1);
            this.unifiedStorage.insert(new ItemStackKey(stack), stack.getCount(), false);
            currentTime = 0;
        }
    }

    private void tickRanchBreeding(ServerLevel level)
    {
        if (level.getGameTime() % RANCH_TICK_INTERVAL != 0) return;

        Set<ResourceLocation> activeTypes = new HashSet<>();
        for (KeyAmount entry : List.copyOf(unifiedStorage.getStorage()))
        {
            if (!(entry.key() instanceof MobStackKey mobKey) || entry.amount() < 2) continue;

            ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mobKey.entityType());
            activeTypes.add(typeId);
            if (!hasBreedingFood(level, mobKey.entityType()))
            {
                resetBreedingTime(typeId);
                continue;
            }

            int breedingTime = mobBreedingTimes.getOrDefault(typeId, 0) + RANCH_TICK_INTERVAL;
            mobBreedingTimes.put(typeId, breedingTime);
            setDirty();
            if (breedingTime < MOB_BREEDING_INTERVAL) continue;

            if (breedStoredMobs(level, mobKey, entry.amount()))
            {
                mobBreedingTimes.put(typeId, 0);
                setDirty();
            }
        }

        if (mobBreedingTimes.keySet().removeIf(typeId -> !activeTypes.contains(typeId)))
            setDirty();
    }

    private boolean hasBreedingFood(ServerLevel level, EntityType<?> entityType)
    {
        if (entityType == EntityType.VILLAGER)
            return getVillagerFoodPoints() >= VILLAGER_FOOD_POINTS;

        ItemStackKey cachedFood = animalFoodKeys.get(entityType);
        if (cachedFood != null)
        {
            if (unifiedStorage.getStackByKey(cachedFood).amount() > 0) return true;
            animalFoodKeys.remove(entityType);
        }

        Entity entity = entityType.create(level);
        if (!(entity instanceof Animal animal)) return false;
        try
        {
            ItemStackKey foodKey = findAnimalFood(animal);
            if (foodKey == null) return false;
            animalFoodKeys.put(entityType, foodKey);
            return true;
        }
        finally
        {
            entity.discard();
        }
    }

    private boolean breedStoredMobs(ServerLevel level, MobStackKey mobKey, long mobCount)
    {
        if (mobKey.entityType() == EntityType.VILLAGER)
            return breedVillagers(mobKey, mobCount);

        ItemStackKey foodKey = animalFoodKeys.get(mobKey.entityType());
        if (foodKey == null) return false;

        long foodCount = unifiedStorage.getStackByKey(foodKey).amount();
        long births = Math.min(mobCount / 2, foodCount / 2);
        births = Math.min(births, Long.MAX_VALUE / 2);
        return completeBreeding(mobKey, births, Map.of(foodKey, births * 2));
    }

    private @Nullable ItemStackKey findAnimalFood(Animal animal)
    {
        for (KeyAmount entry : List.copyOf(unifiedStorage.getStorage()))
        {
            if (entry.amount() > 0 && entry.key() instanceof ItemStackKey itemKey
                    && animal.isFood(itemKey.copyStack()))
                return itemKey;
        }
        return null;
    }

    private boolean breedVillagers(MobStackKey mobKey, long mobCount)
    {
        long births = Math.min(mobCount / 2, getVillagerFoodPoints() / VILLAGER_FOOD_POINTS);
        births = Math.min(births, Long.MAX_VALUE / VILLAGER_FOOD_POINTS);
        births -= unifiedStorage.insert(mobKey, births, true).amount();
        if (births <= 0) return false;

        long requiredPoints = births * VILLAGER_FOOD_POINTS;
        Map<ItemStackKey, Long> costs = new LinkedHashMap<>();
        for (Map.Entry<net.minecraft.world.item.Item, Integer> food : VILLAGER_FOODS.entrySet())
        {
            ItemStackKey foodKey = new ItemStackKey(new ItemStack(food.getKey()));
            long available = unifiedStorage.getStackByKey(foodKey).amount();
            long itemsNeeded = (requiredPoints + food.getValue() - 1) / food.getValue();
            long used = Math.min(available, itemsNeeded);
            if (used <= 0) continue;

            costs.put(foodKey, used);
            requiredPoints = Math.max(0, requiredPoints - used * food.getValue());
            if (requiredPoints == 0) break;
        }
        return requiredPoints == 0 && completeBreeding(mobKey, births, costs);
    }

    private long getVillagerFoodPoints()
    {
        long points = 0;
        for (Map.Entry<net.minecraft.world.item.Item, Integer> food : VILLAGER_FOODS.entrySet())
        {
            long count = unifiedStorage.getStackByKey(new ItemStackKey(new ItemStack(food.getKey()))).amount();
            if (count > (Long.MAX_VALUE - points) / food.getValue()) return Long.MAX_VALUE;
            points += count * food.getValue();
        }
        return points;
    }

    private boolean completeBreeding(MobStackKey mobKey, long births, Map<ItemStackKey, Long> costs)
    {
        if (births <= 0) return false;

        long accepted = births - unifiedStorage.insert(mobKey, births, true).amount();
        if (accepted <= 0) return false;
        if (accepted != births)
        {
            births = accepted;
            if (costs.size() != 1) return false;

            ItemStackKey key = costs.keySet().iterator().next();
            costs = Map.of(key, births * 2);
        }

        Map<ItemStackKey, Long> extractedFoods = new LinkedHashMap<>();
        for (Map.Entry<ItemStackKey, Long> cost : costs.entrySet())
        {
            KeyAmount extracted = unifiedStorage.extract(cost.getKey(), cost.getValue(), false, false);
            if (extracted.amount() != cost.getValue())
            {
                extractedFoods.forEach((key, amount) -> unifiedStorage.insert(key, amount, false));
                if (extracted.amount() > 0) unifiedStorage.insert(cost.getKey(), extracted.amount(), false);
                return false;
            }
            extractedFoods.put(cost.getKey(), extracted.amount());
        }

        long inserted = births - unifiedStorage.insert(mobKey, births, false).amount();
        if (inserted != births)
        {
            if (inserted > 0) unifiedStorage.extract(mobKey, inserted, false, false);
            extractedFoods.forEach((key, amount) -> unifiedStorage.insert(key, amount, false));
            return false;
        }
        return true;
    }

    private void resetBreedingTime(ResourceLocation typeId)
    {
        if (mobBreedingTimes.remove(typeId) != null)
            setDirty();
    }

    private void syncPlayerMembership(UUID playerId, boolean switchPrimary)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || id < 0)
        {
            return;
        }

        PlayerNetIndex.get(server).addMembership(playerId, id, switchPrimary);
    }

    private void syncPlayerRemoval(UUID playerId)
    {
        syncPlayerRemoval(playerId, this.id);
    }

    private static void syncPlayerRemoval(UUID playerId, int netId)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || netId < 0)
        {
            return;
        }

        PlayerNetIndex.get(server).removeMembership(playerId, netId);
    }

    private static String sanitizeCustomName(@Nullable String customName)
    {
        if (customName == null)
            return "";

        String trimmedName = customName.trim();
        if (trimmedName.isEmpty())
            return "";

        StringBuilder builder = new StringBuilder(Math.min(trimmedName.length(), MAX_NETWORK_NAME_LENGTH));
        int appendedCodePoints = 0;
        for (int offset = 0; offset < trimmedName.length() && appendedCodePoints < MAX_NETWORK_NAME_LENGTH; )
        {
            int codePoint = trimmedName.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isISOControl(codePoint))
                continue;

            builder.appendCodePoint(codePoint);
            appendedCodePoints++;
        }
        return builder.toString().trim();
    }
}

