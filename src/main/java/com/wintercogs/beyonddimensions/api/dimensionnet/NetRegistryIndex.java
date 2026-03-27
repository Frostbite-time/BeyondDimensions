package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class NetRegistryIndex extends SavedData
{
    static final String DATA_NAME = "BDNetRegistryIndex";

    private static final String ACTIVE_NET_IDS = "ActiveNetIds";
    private static final String NEXT_NET_ID = "NextNetId";
    private static final String INITIALIZED = "Initialized";
    private static final Pattern LEGACY_NET_FILE_PATTERN = Pattern.compile(Pattern.quote(DimensionsNet.NET_DATA_PREFIX) + "(\\d+)\\.dat");

    private final TreeSet<Integer> activeNetIds = new TreeSet<>();
    private int nextNetId;
    private boolean initialized;

    static NetRegistryIndex get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(NetRegistryIndex::load, NetRegistryIndex::new, DATA_NAME);
    }

    static NetRegistryIndex load(CompoundTag tag)
    {
        NetRegistryIndex index = new NetRegistryIndex();
        ListTag activeIds = tag.getList(ACTIVE_NET_IDS, Tag.TAG_INT);
        for (int i = 0; i < activeIds.size(); i++)
        {
            index.observeExistingNet(activeIds.getInt(i), true);
        }
        if (tag.contains(NEXT_NET_ID, Tag.TAG_INT))
        {
            index.nextNetId = Math.max(0, tag.getInt(NEXT_NET_ID));
        }
        if (tag.contains(INITIALIZED, Tag.TAG_BYTE))
        {
            index.initialized = tag.getBoolean(INITIALIZED);
        }
        return index;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag)
    {
        ListTag activeIdTags = new ListTag();
        for (int netId : activeNetIds)
        {
            activeIdTags.add(IntTag.valueOf(netId));
        }
        tag.put(ACTIVE_NET_IDS, activeIdTags);
        tag.putInt(NEXT_NET_ID, nextNetId);
        tag.putBoolean(INITIALIZED, initialized);
        return tag;
    }

    void ensureInitialized(MinecraftServer server)
    {
        if (initialized)
        {
            return;
        }

        boolean changed = migrateLegacyData(server);
        changed |= !initialized;
        initialized = true;
        if (changed)
        {
            setDirty();
        }
    }

    int allocateNetId(MinecraftServer server)
    {
        ensureInitialized(server);
        return allocateNetId();
    }

    void registerNet(MinecraftServer server, int netId)
    {
        ensureInitialized(server);
        if (registerNet(netId))
        {
            setDirty();
        }
    }

    void unregisterNet(MinecraftServer server, int netId)
    {
        ensureInitialized(server);
        if (activeNetIds.remove(netId))
        {
            setDirty();
        }
    }

    List<Integer> getActiveNetIds(MinecraftServer server)
    {
        ensureInitialized(server);
        return new ArrayList<>(activeNetIds);
    }

    boolean isKnownNet(MinecraftServer server, int netId)
    {
        ensureInitialized(server);
        return activeNetIds.contains(netId);
    }

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
                changed |= observeExistingNet(netId, net != null);
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Failed to initialize net registry index", exception);
        }

        return changed;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        NetRegistryIndex.get(event.getServer()).ensureInitialized(event.getServer());
    }

    private boolean observeExistingNet(int netId, boolean activeNetwork)
    {
        if (netId < 0)
        {
            return false;
        }

        boolean changed = false;
        if (activeNetwork)
        {
            changed = activeNetIds.add(netId);
        }
        if (nextNetId <= netId)
        {
            nextNetId = netId + 1;
            changed = true;
        }
        return changed;
    }

    private boolean registerNet(int netId)
    {
        if (netId < 0)
        {
            return false;
        }

        boolean changed = activeNetIds.add(netId);
        if (netId == nextNetId)
        {
            nextNetId++;
            changed = true;
        }
        return changed;
    }

    private int allocateNetId()
    {
        return nextNetId;
    }
}
