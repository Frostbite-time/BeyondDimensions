package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = BDConstants.MODID)
final class NetRegistryIndex extends SavedData
{
    /**
     * 固定的网络注册表存档名。
     */
    static final String DATA_NAME = "BDNetRegistryIndex";

    private static final String ACTIVE_NET_IDS = "ActiveNetIds";
    private static final String NEXT_NET_ID = "NextNetId";
    private static final String INITIALIZED = "Initialized";
    private static final Pattern LEGACY_NET_FILE_PATTERN = Pattern.compile(Pattern.quote(DimensionsNet.NET_DATA_PREFIX) + "(\\d+)\\.dat");
    private static final Factory<NetRegistryIndex> FACTORY = new Factory<>(NetRegistryIndex::new, NetRegistryIndex::load);

    /**
     * 实际保存已知网络和下一个可分配 ID 的状态容器。
     */
    private final NetRegistryIndexState state = new NetRegistryIndexState();

    /**
     * 获取网络注册表数据，不存在时自动创建。
     *
     * @param server 当前服务端
     * @return 当前世界的网络注册表数据
     */
    static NetRegistryIndex get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * 从磁盘读取网络注册表。
     *
     * @param tag            存档 NBT
     * @param registryAccess 注册表访问器
     * @return 反序列化后的网络注册表
     */
    static NetRegistryIndex load(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        NetRegistryIndex index = new NetRegistryIndex();
        ListTag activeNetIds = tag.getList(ACTIVE_NET_IDS, IntTag.TAG_INT);
        for (int i = 0; i < activeNetIds.size(); i++)
        {
            index.state.observeExistingNet(activeNetIds.getInt(i), true);
        }
        if (tag.contains(NEXT_NET_ID))
        {
            index.state.observeExistingNet(tag.getInt(NEXT_NET_ID) - 1, false);
        }
        if (tag.contains(INITIALIZED))
        {
            index.state.setInitialized(tag.getBoolean(INITIALIZED));
        }
        return index;
    }

    /**
     * 把网络注册表写回磁盘。
     *
     * @param tag            待写入的 NBT
     * @param registryAccess 注册表访问器
     * @return 写入后的 NBT
     */
    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registryAccess)
    {
        ListTag activeNetIds = new ListTag();
        for (int netId : state.getActiveNetIds())
        {
            activeNetIds.add(IntTag.valueOf(netId));
        }
        tag.put(ACTIVE_NET_IDS, activeNetIds);
        tag.putInt(NEXT_NET_ID, state.getNextNetId());
        tag.putBoolean(INITIALIZED, state.isInitialized());
        return tag;
    }

    /**
     * 确保网络注册表已完成初始化。
     * <p>
     * 对旧存档会通过扫描 `world/data` 目录下的网络数据文件完成一次迁移；
     * 初始化完成后，后续运行不再依赖范围扫描。
     *
     * @param server 当前服务端
     */
    void ensureInitialized(MinecraftServer server)
    {
        if (state.isInitialized())
        {
            return;
        }

        boolean changed = migrateLegacyData(server);
        changed |= state.setInitialized(true);
        if (changed)
        {
            setDirty();
        }
    }

    /**
     * 分配一个新的网络 ID。
     * <p>
     * 该操作只负责提供一个当前可用的候选 ID，不会自动推进下一个 ID，
     * 也不会自动把它加入有效网络集合；只有当该 ID 被显式注册成功后，
     * 下一个候选 ID 才会增长。
     * 调用方需要在网络真正创建成功后显式调用 {@link #registerNet(MinecraftServer, int)}。
     *
     * @param server 当前服务端
     * @return 新分配的网络 ID
     */
    int allocateNetId(MinecraftServer server)
    {
        ensureInitialized(server);
        int allocated = state.allocateNetId();
        setDirty();
        return allocated;
    }

    /**
     * 登记一个有效网络。
     *
     * @param server 当前服务端
     * @param netId  网络 ID
     */
    void registerNet(MinecraftServer server, int netId)
    {
        ensureInitialized(server);
        if (state.registerNet(netId))
        {
            setDirty();
        }
    }

    /**
     * 注销一个有效网络。
     *
     * @param server 当前服务端
     * @param netId  网络 ID
     */
    void unregisterNet(MinecraftServer server, int netId)
    {
        ensureInitialized(server);
        if (state.unregisterNet(netId))
        {
            setDirty();
        }
    }

    /**
     * 获取全部有效网络 ID。
     *
     * @param server 当前服务端
     * @return 从小到大排序的有效网络 ID 列表
     */
    List<Integer> getActiveNetIds(MinecraftServer server)
    {
        ensureInitialized(server);
        return state.getActiveNetIds();
    }

    /**
     * 判断某个网络是否已登记为有效网络。
     *
     * @param server 当前服务端
     * @param netId  网络 ID
     * @return 网络仍处于有效集合时返回 {@code true}
     */
    boolean isKnownNet(MinecraftServer server, int netId)
    {
        ensureInitialized(server);
        return state.isKnownNet(netId);
    }

    /**
     * 从旧版网络存档文件迁移出网络注册表信息。
     *
     * @param server 当前服务端
     * @return 迁移过程中有状态变更时返回 {@code true}
     */
    private boolean migrateLegacyData(MinecraftServer server)
    {
        boolean changed = false;
        Path dataPath = server.getWorldPath(LevelResource.ROOT).resolve("data");
        if (!Files.isDirectory(dataPath))
        {
            return false;
        }

        try (var paths = Files.list(dataPath))
        {
            for (Path path : (Iterable<Path>) paths::iterator)
            {
                Matcher matcher = LEGACY_NET_FILE_PATTERN.matcher(path.getFileName().toString());
                if (!matcher.matches())
                {
                    continue;
                }

                int netId = Integer.parseInt(matcher.group(1));
                DimensionsNet net = DimensionsNet.getNetFromId(server, netId);
                changed |= state.observeExistingNet(netId, net != null);
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to initialize net registry index", exception);
        }

        return changed;
    }

    /**
     * 服务端启动后初始化网络注册表。
     * <p>
     * 旧存档会在这里完成一次网络注册表迁移，以便后续逻辑不再依赖范围扫描。
     *
     * @param event 服务端启动事件
     */
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event)
    {
        NetRegistryIndex.get(event.getServer()).ensureInitialized(event.getServer());
    }
}
