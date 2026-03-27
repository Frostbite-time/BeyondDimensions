package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.*;

@EventBusSubscriber(modid = BDConstants.MODID)
final class PlayerNetIndex extends SavedData
{
    static final String DATA_NAME = "bd_player_net_index";
    static final int NO_PRIMARY_NET = -1;

    private static final String PRIMARY_NET_ENTRIES = "primary_net_entries";
    private static final String PLAYER_ID = "player_id";
    private static final String PRIMARY_NET_ID = "primary_net_id";
    private static final Codec<PlayerNetIndex> CODEC = CompoundTag.CODEC.xmap(PlayerNetIndex::fromTag, PlayerNetIndex::toTag);
    private static final SavedDataType<PlayerNetIndex> SAVED_DATA_TYPE = new SavedDataType<>(
            BeyondDimensions.makeId(DATA_NAME),
            PlayerNetIndex::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<UUID, Integer> primaryNetIds = new HashMap<>();
    private final Map<UUID, LinkedHashSet<Integer>> allNetIds = new HashMap<>();

    static PlayerNetIndex get(MinecraftServer server)
    {
        return server.getDataStorage().computeIfAbsent(SAVED_DATA_TYPE);
    }

    static PlayerNetIndex getIfPresent(MinecraftServer server)
    {
        return server.getDataStorage().get(SAVED_DATA_TYPE);
    }

    private static PlayerNetIndex fromTag(CompoundTag tag)
    {
        PlayerNetIndex index = new PlayerNetIndex();
        ListTag entryList = tag.getListOrEmpty(PRIMARY_NET_ENTRIES);
        entryList.forEach(element -> element.asCompound().ifPresent(entry -> {
            String playerId = entry.getStringOr(PLAYER_ID, "");
            if (playerId.isEmpty() || !entry.contains(PRIMARY_NET_ID))
            {
                return;
            }
            index.primaryNetIds.put(UUID.fromString(playerId), entry.getIntOr(PRIMARY_NET_ID, NO_PRIMARY_NET));
        }));
        return index;
    }

    private CompoundTag toTag()
    {
        CompoundTag tag = new CompoundTag();
        ListTag entryList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : copyPrimaryNetIds().entrySet())
        {
            CompoundTag data = new CompoundTag();
            data.putString(PLAYER_ID, entry.getKey().toString());
            data.putInt(PRIMARY_NET_ID, entry.getValue());
            entryList.add(data);
        }
        tag.put(PRIMARY_NET_ENTRIES, entryList);
        return tag;
    }

    void clearRuntime()
    {
        allNetIds.clear();
    }

    void rebuildFromServer(MinecraftServer server)
    {
        clearRuntime();
        for (int netId : NetRegistryIndex.get(server).getActiveNetIds(server))
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

    void addMembership(UUID playerId, int netId, boolean switchPrimary)
    {
        if (netId < 0)
        {
            return;
        }

        LinkedHashSet<Integer> memberships = allNetIds.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>());
        if (memberships.add(netId))
        {
            if (switchPrimary)
            {
                primaryNetIds.put(playerId, netId);
            }
            setDirty();
        }
    }

    void removeMembership(UUID playerId, int netId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        if (memberships == null || !memberships.remove(netId))
        {
            return;
        }

        if (memberships.isEmpty())
        {
            allNetIds.remove(playerId);
            primaryNetIds.remove(playerId);
            setDirty();
            return;
        }

        Integer primaryNetId = primaryNetIds.get(playerId);
        if (primaryNetId != null && primaryNetId == netId)
        {
            primaryNetIds.put(playerId, getSmallestNetId(memberships));
        }
        setDirty();
    }

    void clearPrimary(UUID playerId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        if (memberships == null || memberships.isEmpty())
        {
            if (primaryNetIds.remove(playerId) != null)
            {
                setDirty();
            }
            return;
        }

        Integer previous = primaryNetIds.put(playerId, NO_PRIMARY_NET);
        if (previous == null || previous != NO_PRIMARY_NET)
        {
            setDirty();
        }
    }

    boolean setPrimary(UUID playerId, int netId)
    {
        boolean changed;
        if (netId == NO_PRIMARY_NET)
        {
            LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
            if (memberships == null || memberships.isEmpty())
            {
                changed = primaryNetIds.remove(playerId) != null;
            }
            else
            {
                Integer previous = primaryNetIds.put(playerId, NO_PRIMARY_NET);
                changed = previous == null || previous != NO_PRIMARY_NET;
            }
        }
        else
        {
            LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
            if (memberships == null || !memberships.contains(netId))
            {
                return false;
            }

            Integer previous = primaryNetIds.put(playerId, netId);
            changed = previous == null || previous != netId;
        }

        if (changed)
        {
            setDirty();
        }
        return changed;
    }

    int getPrimaryNetId(UUID playerId)
    {
        return primaryNetIds.getOrDefault(playerId, NO_PRIMARY_NET);
    }

    boolean hasAnyMembership(UUID playerId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        return memberships != null && !memberships.isEmpty();
    }

    List<Integer> getAllNetIds(UUID playerId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        return memberships == null || memberships.isEmpty() ? List.of() : new ArrayList<>(memberships);
    }

    Map<UUID, Integer> copyPrimaryNetIds()
    {
        return new HashMap<>(primaryNetIds);
    }

    boolean reconcilePrimaryMappings()
    {
        boolean changed = false;
        Set<UUID> playerIds = new HashSet<>(allNetIds.keySet());
        playerIds.addAll(primaryNetIds.keySet());

        for (UUID playerId : playerIds)
        {
            LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
            if (memberships == null || memberships.isEmpty())
            {
                changed |= primaryNetIds.remove(playerId) != null;
                continue;
            }

            if (!primaryNetIds.containsKey(playerId))
            {
                primaryNetIds.put(playerId, getSmallestNetId(memberships));
                changed = true;
                continue;
            }

            int primaryNetId = primaryNetIds.get(playerId);
            if (primaryNetId == NO_PRIMARY_NET)
            {
                continue;
            }

            if (!memberships.contains(primaryNetId))
            {
                primaryNetIds.put(playerId, getSmallestNetId(memberships));
                changed = true;
            }
        }
        return changed;
    }

    private static int getSmallestNetId(LinkedHashSet<Integer> memberships)
    {
        int smallest = Integer.MAX_VALUE;
        for (int membership : memberships)
        {
            smallest = Math.min(smallest, membership);
        }
        return smallest;
    }

    @SubscribeEvent
    private static void onServerStarted(ServerStartedEvent event)
    {
        PlayerNetIndex.get(event.getServer()).rebuildFromServer(event.getServer());
    }
}
