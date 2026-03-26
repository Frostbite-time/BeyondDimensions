package com.wintercogs.beyonddimensions.api.dimensionnet;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

final class NetRegistryIndexState
{
    /**
     * 当前仍然有效的网络 ID 集合。
     */
    private final TreeSet<Integer> activeNetIds = new TreeSet<>();

    /**
     * 下一个将被分配的新网络 ID。
     */
    private int nextNetId;

    /**
     * 标记该注册表是否已经完成初始化或旧存档迁移。
     */
    private boolean initialized;

    /**
     * 记录一个已经存在的网络 ID。
     * <p>
     * 该方法会推进 {@code nextNetId}，以避免与历史 ID 冲突。
     *
     * @param netId         已存在的网络 ID
     * @param activeNetwork 为真时把该网络视为当前有效网络
     * @return 注册表状态发生变化时返回 {@code true}
     */
    boolean observeExistingNet(int netId, boolean activeNetwork)
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

    /**
     * 分配一个新的可用网络 ID。
     *
     * @return 新分配的网络 ID
     */
    int allocateNetId()
    {
        return nextNetId;
    }

    /**
     * 把一个网络标记为当前有效。并在适当时机推进 {@code nextNetId}
     *
     * @param netId 网络 ID
     * @return 集合内容发生变化时返回 {@code true}
     */
    boolean registerNet(int netId)
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

    /**
     * 把一个网络从当前有效集合中移除。
     * <p>
     * 该操作不会回收已分配 ID，也不会回退 {@code nextNetId}。
     *
     * @param netId 网络 ID
     * @return 集合内容发生变化时返回 {@code true}
     */
    boolean unregisterNet(int netId)
    {
        return activeNetIds.remove(netId);
    }

    /**
     * 判断一个网络当前是否被视为有效网络。
     *
     * @param netId 网络 ID
     * @return 网络位于有效集合中时返回 {@code true}
     */
    boolean isKnownNet(int netId)
    {
        return activeNetIds.contains(netId);
    }

    /**
     * 获取全部有效网络 ID 的有序副本。
     *
     * @return 从小到大排序的有效网络 ID 列表
     */
    List<Integer> getActiveNetIds()
    {
        return new ArrayList<>(activeNetIds);
    }

    /**
     * 获取当前下一个可分配网络 ID。
     *
     * @return 下一个可分配网络 ID
     */
    int getNextNetId()
    {
        return nextNetId;
    }

    /**
     * 判断注册表是否已经初始化。
     *
     * @return 已初始化时返回 {@code true}
     */
    boolean isInitialized()
    {
        return initialized;
    }

    /**
     * 更新注册表初始化标记。
     *
     * @param initialized 新的初始化状态
     * @return 初始化标记发生变化时返回 {@code true}
     */
    boolean setInitialized(boolean initialized)
    {
        boolean changed = this.initialized != initialized;
        this.initialized = initialized;
        return changed;
    }
}
