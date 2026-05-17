package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.config.ServerConfigRuntime;
import com.wintercogs.beyonddimensions.util.PlayerNameHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
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
    static final String NET_DATA_PREFIX = "bd_net_";
    public static final int NO_PRIMARY_NET_ID = -1;
    public static final int MAX_NETWORK_NAME_LENGTH = 48;
    private static final int DELETED_ID = -99;

    private static final String SAVE_KEY_ID = "id";
    private static final String SAVE_KEY_OWNER = "owner";
    private static final String SAVE_KEY_MANAGERS = "managers";
    private static final String SAVE_KEY_PLAYERS = "players";
    private static final String SAVE_KEY_UNIFIED_STORAGE = "unified_storage";
    private static final String SAVE_KEY_CURRENT_TIME = "current_time";
    private static final String SAVE_KEY_DELETED = "deleted";
    private static final String SAVE_KEY_CUSTOM_NAME = "custom_name";

    private static final Codec<DimensionsNet> CODEC = CompoundTag.CODEC.xmap(DimensionsNet::fromTag, DimensionsNet::toTag);

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

    private static SavedDataType<@NotNull DimensionsNet> savedDataType(String netName)
    {
        return new SavedDataType<>(BeyondDimensions.makeId(netName),
                DimensionsNet::create, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    private static HolderLookup.Provider currentRegistryAccess()
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            throw new IllegalStateException("Cannot access registry provider when server is unavailable");
        }
        return server.registryAccess();
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

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            int allocatedNetId = NetRegistryIndex.get(server).allocateNetId(server);
            String netDataName = DimensionsNet.buildNetDataName(allocatedNetId);
            DimensionsNet newNet = server.getDataStorage().computeIfAbsent(savedDataType(netDataName));
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
     * @return 最新可用的网络名称，内容为字符串："bd_net_<数字id>"
     */
    public static String buildNewNetName(@NotNull MinecraftServer dataProvider)
    {
        return buildNetDataName(NetRegistryIndex.get(dataProvider).allocateNetId(dataProvider));
    }

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

        DimensionsNet net = server.getDataStorage().get(savedDataType(buildNetDataName(id)));
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

    public static @Nullable DimensionsNet getPrimaryNetFromPlayer(Player player)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        PlayerNetIndex index = PlayerNetIndex.get(server);
        int primaryNetId = index.getPrimaryNetId(player.getUUID());
        if (primaryNetId == PlayerNetIndex.NO_PRIMARY_NET)
        {
            return null;
        }

        return getNetFromId(server, primaryNetId);
    }

    public static @NotNull List<DimensionsNet> getAllNetFromPlayer(Player player)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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

    public static boolean hasAnyNet(Player player)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && PlayerNetIndex.get(server).hasAnyMembership(player.getUUID());
    }

    public static boolean hasPrimaryNet(Player player)
    {
        return getPrimaryNetFromPlayer(player) != null;
    }

    public static boolean setPrimaryNetForPlayer(Player player, @Nullable DimensionsNet net)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            return false;
        }

        PlayerNetIndex index = PlayerNetIndex.get(server);
        return net == null
                ? index.setPrimary(player.getUUID(), PlayerNetIndex.NO_PRIMARY_NET)
                : index.setPrimary(player.getUUID(), net.getId());
    }

    public static void clearPrimaryNetForPlayer(Player player)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            return;
        }

        PlayerNetIndex.get(server).clearPrimary(player.getUUID());
    }

    private static DimensionsNet fromTag(CompoundTag tag)
    {
        DimensionsNet net = new DimensionsNet(false);

        net.id = tag.getIntOr(SAVE_KEY_ID, DELETED_ID);
        net.customName = sanitizeCustomName(tag.getStringOr(SAVE_KEY_CUSTOM_NAME, ""));
        String owner = tag.getStringOr(SAVE_KEY_OWNER, "");
        if (!owner.isEmpty())
        {
            net.owner = UUID.fromString(owner);
        }

        net.unifiedStorage.deserializeNBT(currentRegistryAccess(), tag.getCompoundOrEmpty(SAVE_KEY_UNIFIED_STORAGE));

        if (tag.contains("EnergyStorage"))
        {
            CompoundTag energyTag = tag.getCompoundOrEmpty("EnergyStorage");
            if (energyTag.contains("Energy"))
            {
                net.unifiedStorage.insert(EnergyStackKey.INSTANCE, energyTag.getLongOr("Energy", 0L), false);
            }
        }

        if (tag.contains(SAVE_KEY_MANAGERS))
        {
            ListTag managerList = tag.getListOrEmpty(SAVE_KEY_MANAGERS);
            managerList.forEach(manager -> manager.asString().ifPresent(managerId -> net.managers.add(UUID.fromString(managerId))));
        }

        if (tag.contains(SAVE_KEY_PLAYERS))
        {
            ListTag playerList = tag.getListOrEmpty(SAVE_KEY_PLAYERS);
            playerList.forEach(player -> player.asString().ifPresent(playerId -> net.players.add(UUID.fromString(playerId))));
        }

        net.currentTime = tag.getIntOr(SAVE_KEY_CURRENT_TIME, 0);
        net.deleted = tag.getBooleanOr(SAVE_KEY_DELETED, false);

        return net;
    }

    private CompoundTag toTag()
    {
        CompoundTag tag = new CompoundTag();

        tag.putInt(SAVE_KEY_ID, this.id);
        if (!this.customName.isEmpty())
        {
            tag.putString(SAVE_KEY_CUSTOM_NAME, this.customName);
        }
        if (this.owner != null)
        {
            tag.putString(SAVE_KEY_OWNER, this.owner.toString());
        }

        ListTag managerListTag = new ListTag();
        for (UUID manager : managers)
        {
            managerListTag.add(StringTag.valueOf(manager.toString()));
        }
        tag.put(SAVE_KEY_MANAGERS, managerListTag);

        ListTag playerListTag = new ListTag();
        for (UUID player : players)
        {
            playerListTag.add(StringTag.valueOf(player.toString()));
        }
        tag.put(SAVE_KEY_PLAYERS, playerListTag);

        tag.put(SAVE_KEY_UNIFIED_STORAGE, unifiedStorage.serializeNBT(currentRegistryAccess()));
        tag.putInt(SAVE_KEY_CURRENT_TIME, this.currentTime);
        tag.putBoolean(SAVE_KEY_DELETED, this.deleted);

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
        {
            return Component.literal(sanitizedName);
        }

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
        {
            return;
        }

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
        this.id = DELETED_ID; // 用-99作为被删除的特殊标记
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
        // 不对临时网络执行倒计时
        if (temporary || ServerConfigRuntime.crystalGenerateTime <= 0)
            return;

        currentTime++;
        setDirty();
        if (currentTime >= ServerConfigRuntime.crystalGenerateTime * 20)
        {
            ItemStack stack = new ItemStack(BDItems.SHATTERED_SPACE_TIME_CRYSTALLIZATION.get(), 1);
            this.unifiedStorage.insert(new ItemStackKey(stack), stack.getCount(), false);
            currentTime = 0;
        }

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
        {
            return "";
        }

        String trimmedName = customName.trim();
        if (trimmedName.isEmpty())
        {
            return "";
        }

        StringBuilder builder = new StringBuilder(Math.min(trimmedName.length(), MAX_NETWORK_NAME_LENGTH));
        int appendedCodePoints = 0;
        for (int offset = 0; offset < trimmedName.length() && appendedCodePoints < MAX_NETWORK_NAME_LENGTH; )
        {
            int codePoint = trimmedName.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isISOControl(codePoint))
            {
                continue;
            }

            builder.appendCodePoint(codePoint);
            appendedCodePoints++;
        }
        return builder.toString().trim();
    }
}

