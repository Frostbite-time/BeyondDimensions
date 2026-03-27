package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = BDConstants.MODID)
final class NetRegistryIndex extends SavedData
{
    static final String DATA_NAME = "bd_net_registry_index";

    private static final String ACTIVE_NET_IDS = "active_net_ids";
    private static final String NEXT_NET_ID = "next_net_id";
    private static final String INITIALIZED = "initialized";
    private static final Pattern LEGACY_NET_FILE_PATTERN = Pattern.compile(Pattern.quote(DimensionsNet.NET_DATA_PREFIX) + "(\\d+)\\.dat");
    private static final Codec<NetRegistryIndex> CODEC = CompoundTag.CODEC.xmap(NetRegistryIndex::fromTag, NetRegistryIndex::toTag);
    private static final SavedDataType<NetRegistryIndex> SAVED_DATA_TYPE = new SavedDataType<>(
            BeyondDimensions.makeId(DATA_NAME),
            NetRegistryIndex::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final TreeSet<Integer> activeNetIds = new TreeSet<>();
    private int nextNetId;
    private boolean initialized;

    static NetRegistryIndex get(MinecraftServer server)
    {
        return server.getDataStorage().computeIfAbsent(SAVED_DATA_TYPE);
    }

    private static NetRegistryIndex fromTag(CompoundTag tag)
    {
        NetRegistryIndex index = new NetRegistryIndex();
        ListTag activeNetIds = tag.getListOrEmpty(ACTIVE_NET_IDS);
        activeNetIds.forEach(element -> element.asInt().ifPresent(netId -> index.observeExistingNet(netId, true)));
        index.nextNetId = Math.max(0, tag.getIntOr(NEXT_NET_ID, 0));
        index.initialized = tag.getBooleanOr(INITIALIZED, false);
        return index;
    }

    private CompoundTag toTag()
    {
        CompoundTag tag = new CompoundTag();
        ListTag activeNetIdTags = new ListTag();
        for (int netId : activeNetIds)
        {
            activeNetIdTags.add(IntTag.valueOf(netId));
        }
        tag.put(ACTIVE_NET_IDS, activeNetIdTags);
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
    private static void onServerStarted(ServerStartedEvent event)
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
