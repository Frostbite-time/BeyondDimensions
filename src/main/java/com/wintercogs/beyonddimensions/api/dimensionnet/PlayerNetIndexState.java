package com.wintercogs.beyonddimensions.api.dimensionnet;

import java.util.*;

final class PlayerNetIndexState
{
    /**
     * 表示玩家当前没有主网络的特殊值。
     */
    static final int NO_PRIMARY_NET = -1;

    /**
     * 玩家 UUID -> 主网络 ID。
     * <p>
     * 该映射可被持久化；值为 {@link #NO_PRIMARY_NET} 时表示空主网络状态。
     */
    private final Map<UUID, Integer> primaryNetIds = new HashMap<>();

    /**
     * 玩家 UUID -> 全部成员网络 ID 组。
     * <p>
     * 该映射只在运行时维护，不会写入存档。
     */
    private final Map<UUID, LinkedHashSet<Integer>> allNetIds = new HashMap<>();

    /**
     * 清空所有运行时成员网络组。
     */
    void clearRuntime()
    {
        allNetIds.clear();
    }

    /**
     * 写入一条从存档读取到的主网络映射。
     *
     * @param playerId 玩家 UUID
     * @param netId    主网络 ID
     */
    void putSavedPrimary(UUID playerId, int netId)
    {
        primaryNetIds.put(playerId, netId);
    }

    /**
     * 向运行时成员网络组中加入一条成员关系。
     *
     * @param playerId      玩家 UUID
     * @param netId         网络 ID
     * @param switchPrimary 为真时，加入成功后自动把该网络设为主网络
     * @return 成员关系首次加入成功时返回 {@code true}
     */
    boolean addMembership(UUID playerId, int netId, boolean switchPrimary)
    {
        if (netId < 0)
        {
            return false;
        }

        LinkedHashSet<Integer> memberships = allNetIds.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>());
        boolean added = memberships.add(netId);
        if (added)
        {
            if (switchPrimary)
            {
                primaryNetIds.put(playerId, netId);
            }
            return true;
        }

        return false;
    }

    /**
     * 从运行时成员网络组中移除一条成员关系。
     * <p>
     * 如果移除的是当前主网络，则自动切换到序号最小的剩余网络。
     *
     * @param playerId 玩家 UUID
     * @param netId    网络 ID
     * @return 成员关系实际被移除时返回 {@code true}
     */
    boolean removeMembership(UUID playerId, int netId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        if (memberships == null)
        {
            return false;
        }

        boolean removed = memberships.remove(netId);
        if (!removed)
        {
            return false;
        }

        if (memberships.isEmpty())
        {
            allNetIds.remove(playerId);
            primaryNetIds.remove(playerId);
            return true;
        }

        Integer primaryNetId = primaryNetIds.get(playerId);
        if (primaryNetId != null && primaryNetId == netId)
        {
            primaryNetIds.put(playerId, getSmallestNetId(memberships));
        }
        return true;
    }

    /**
     * 将玩家切换到空主网络状态。
     *
     * @param playerId 玩家 UUID
     * @return 主网络状态发生变化时返回 {@code true}
     */
    boolean clearPrimary(UUID playerId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        if (memberships == null || memberships.isEmpty())
        {
            return primaryNetIds.remove(playerId) != null;
        }

        Integer previous = primaryNetIds.put(playerId, NO_PRIMARY_NET);
        return previous == null || previous != NO_PRIMARY_NET;
    }

    /**
     * 设置玩家主网络。
     *
     * @param playerId 玩家 UUID
     * @param netId    目标主网络 ID；传入 {@link #NO_PRIMARY_NET} 表示清空主网络
     * @return 主网络状态发生变化时返回 {@code true}
     */
    boolean setPrimary(UUID playerId, int netId)
    {
        if (netId == NO_PRIMARY_NET)
        {
            return clearPrimary(playerId);
        }

        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        if (memberships == null || !memberships.contains(netId))
        {
            return false;
        }

        Integer previous = primaryNetIds.put(playerId, netId);
        return previous == null || previous != netId;
    }

    /**
     * 获取玩家主网络 ID。
     *
     * @param playerId 玩家 UUID
     * @return 主网络 ID；若没有主网络则返回 {@link #NO_PRIMARY_NET}
     */
    int getPrimaryNetId(UUID playerId)
    {
        return primaryNetIds.getOrDefault(playerId, NO_PRIMARY_NET);
    }

    /**
     * 判断玩家是否仍然属于任意网络。
     *
     * @param playerId 玩家 UUID
     * @return 玩家存在至少一个成员网络时返回 {@code true}
     */
    boolean hasAnyMembership(UUID playerId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        return memberships != null && !memberships.isEmpty();
    }

    /**
     * 获取玩家当前所属的全部网络 ID。
     *
     * @param playerId 玩家 UUID
     * @return 全部成员网络 ID 列表
     */
    List<Integer> getAllNetIds(UUID playerId)
    {
        LinkedHashSet<Integer> memberships = allNetIds.get(playerId);
        if (memberships == null || memberships.isEmpty())
        {
            return List.of();
        }

        return new ArrayList<>(memberships);
    }

    /**
     * 复制一份当前主网络映射。
     *
     * @return 玩家 UUID 到主网络 ID 的映射副本
     */
    Map<UUID, Integer> copyPrimaryNetIds()
    {
        return new HashMap<>(primaryNetIds);
    }

    /**
     * 校验并修正主网络映射。
     * <p>
     * 如果玩家有成员网络但没有主网络记录，或者原主网络已经失效，
     * 则自动回退到序号最小的有效网络。
     *
     * @return 修正过程中有实际变更时返回 {@code true}
     */
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

    /**
     * 获取一组网络 ID 中序号最小的那个。
     *
     * @param memberships 候选网络 ID 集合
     * @return 最小的网络 ID
     */
    private static int getSmallestNetId(LinkedHashSet<Integer> memberships)
    {
        int smallest = Integer.MAX_VALUE;
        for (int membership : memberships)
        {
            smallest = Math.min(smallest, membership);
        }
        return smallest;
    }
}
