package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = BDConstants.MODID)
final class PlayerNetIndex extends SavedData
{
    /**
     * 固定的玩家主网络索引存档名。
     */
    static final String DATA_NAME = "BDPlayerNetIndex";

    /**
     * 表示玩家当前没有主网络的特殊值。
     */
    static final int NO_PRIMARY_NET = PlayerNetIndexState.NO_PRIMARY_NET;

    private static final String PRIMARY_NET_ENTRIES = "PrimaryNetEntries";
    private static final String PLAYER_ID = "PlayerId";
    private static final String PRIMARY_NET_ID = "PrimaryNetId";
    private static final Factory<PlayerNetIndex> FACTORY = new Factory<>(PlayerNetIndex::new, PlayerNetIndex::load);

    /**
     * 持久化主网络映射与运行时成员网络组的实际状态容器。
     */
    private final PlayerNetIndexState state = new PlayerNetIndexState();

    /**
     * 获取玩家网络索引数据，不存在时自动创建。
     *
     * @param server 当前服务端
     * @return 服务端对应的玩家网络索引数据
     */
    static PlayerNetIndex get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * 获取已经加载的玩家网络索引数据，但不会自动创建。
     *
     * @param server 当前服务端
     * @return 已存在的玩家网络索引；若尚未加载则返回 {@code null}
     */
    static PlayerNetIndex getIfPresent(MinecraftServer server)
    {
        return server.overworld().getDataStorage().get(FACTORY, DATA_NAME);
    }

    /**
     * 从磁盘读取玩家主网络映射。
     * <p>
     * 这里只恢复持久化的主网络信息；运行时成员网络组会在服务端启动后重建。
     *
     * @param tag            存档 NBT
     * @param registryAccess 注册表访问器
     * @return 反序列化后的索引数据
     */
    static PlayerNetIndex load(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        PlayerNetIndex index = new PlayerNetIndex();
        ListTag entryList = tag.getList(PRIMARY_NET_ENTRIES, 10);
        for (int i = 0; i < entryList.size(); i++)
        {
            CompoundTag entry = entryList.getCompound(i);
            if (!entry.hasUUID(PLAYER_ID) || !entry.contains(PRIMARY_NET_ID))
            {
                continue;
            }
            index.state.putSavedPrimary(entry.getUUID(PLAYER_ID), entry.getInt(PRIMARY_NET_ID));
        }
        return index;
    }

    /**
     * 把玩家主网络映射序列化到磁盘。
     * <p>
     * 运行时成员网络组不会写入存档。
     *
     * @param tag            待写入的 NBT
     * @param registryAccess 注册表访问器
     * @return 写入后的 NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        ListTag entryList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : state.copyPrimaryNetIds().entrySet())
        {
            CompoundTag data = new CompoundTag();
            data.putUUID(PLAYER_ID, entry.getKey());
            data.putInt(PRIMARY_NET_ID, entry.getValue());
            entryList.add(data);
        }
        tag.put(PRIMARY_NET_ENTRIES, entryList);
        return tag;
    }

    /**
     * 清空运行时成员网络组。
     * <p>
     * 该操作不会清除已经持久化的主网络映射。
     */
    void clearRuntime()
    {
        state.clearRuntime();
    }

    /**
     * 通过扫描现有维度网络重建运行时成员网络组。
     * <p>
     * 重建完成后会校验持久化主网络：若主网络失效，则自动回退到序号最小的有效网络。
     *
     * @param server 当前服务端
     */
    void rebuildFromServer(MinecraftServer server)
    {
        clearRuntime();
        for (int netId = 0; netId < DimensionsNet.MAX_NET_SCAN; netId++)
        {
            DimensionsNet net = DimensionsNet.getNetFromId(server, netId);
            if (net == null)
            {
                continue;
            }
            for (UUID playerId : net.getPlayers())
            {
                addMembership(playerId, net.getId(), false);
            }
        }
        if (reconcilePrimaryMappings())
        {
            setDirty();
        }
    }

    /**
     * 向运行时成员网络组中加入一条玩家成员关系。
     *
     * @param playerId      玩家 UUID
     * @param netId         网络 ID
     * @param switchPrimary 为真时，加入后自动切换主网络
     */
    void addMembership(UUID playerId, int netId, boolean switchPrimary)
    {
        if (state.addMembership(playerId, netId, switchPrimary))
        {
            setDirty();
        }
    }

    /**
     * 从运行时成员网络组中移除一条玩家成员关系。
     * <p>
     * 如果移除的是当前主网络，会自动回退到序号最小的剩余网络。
     *
     * @param playerId 玩家 UUID
     * @param netId    网络 ID
     */
    void removeMembership(UUID playerId, int netId)
    {
        if (state.removeMembership(playerId, netId))
        {
            setDirty();
        }
    }

    /**
     * 将玩家切换到空主网络状态。
     * <p>
     * 该操作不会移除玩家仍然拥有的其他成员网络。
     *
     * @param playerId 玩家 UUID
     */
    void clearPrimary(UUID playerId)
    {
        if (state.clearPrimary(playerId))
        {
            setDirty();
        }
    }

    /**
     * 设置玩家主网络。
     *
     * @param playerId 玩家 UUID
     * @param netId    目标主网络 ID；传入 {@link #NO_PRIMARY_NET} 表示清空主网络
     * @return 主网络状态实际发生变化时返回 {@code true}
     */
    boolean setPrimary(UUID playerId, int netId)
    {
        boolean changed = state.setPrimary(playerId, netId);
        if (changed)
        {
            setDirty();
        }
        return changed;
    }

    /**
     * 获取玩家主网络 ID。
     *
     * @param playerId 玩家 UUID
     * @return 主网络 ID；若没有主网络则返回 {@link #NO_PRIMARY_NET}
     */
    int getPrimaryNetId(UUID playerId)
    {
        return state.getPrimaryNetId(playerId);
    }

    /**
     * 判断玩家是否仍然属于任意网络。
     *
     * @param playerId 玩家 UUID
     * @return 玩家拥有至少一条成员关系时返回 {@code true}
     */
    boolean hasAnyMembership(UUID playerId)
    {
        return state.hasAnyMembership(playerId);
    }

    /**
     * 获取玩家当前所属的全部网络 ID。
     *
     * @param playerId 玩家 UUID
     * @return 运行时维护的全部网络 ID 列表
     */
    List<Integer> getAllNetIds(UUID playerId)
    {
        return state.getAllNetIds(playerId);
    }

    /**
     * 复制一份当前已持久化的主网络映射。
     *
     * @return 玩家 UUID 到主网络 ID 的映射副本
     */
    Map<UUID, Integer> copyPrimaryNetIds()
    {
        return state.copyPrimaryNetIds();
    }

    /**
     * 校验并修正主网络映射。
     * <p>
     * 当主网络缺失或已经不再属于玩家时，会自动回退到序号最小的有效网络。
     *
     * @return 修正过程中有实际变更时返回 {@code true}
     */
    boolean reconcilePrimaryMappings()
    {
        return state.reconcilePrimaryMappings();
    }

    /**
     * 服务端启动后重建运行时索引。
     * <p>
     * 注：服务器关闭时无需显式清空，因为我们只会在服务器启动后才使用内容。而每次启动都会自动重建。
     *
     * @param event 服务端启动事件
     */
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event)
    {
        PlayerNetIndex.get(event.getServer()).rebuildFromServer(event.getServer());
    }
}
