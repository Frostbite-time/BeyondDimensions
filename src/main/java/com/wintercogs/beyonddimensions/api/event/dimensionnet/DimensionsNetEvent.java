package com.wintercogs.beyonddimensions.api.event.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.neoforged.bus.api.Event;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 维度网络相关事件
 */
public abstract class DimensionsNetEvent extends Event
{
    private final DimensionsNet net;

    public DimensionsNetEvent(DimensionsNet net)
    {
        this.net = net;
    }

    public DimensionsNet getNet()
    {
        return net;
    }

    /**
     * 创建维度网络的事件，当此事件发送时，维度网络已被创建并初始化完成
     * <p>
     * 用途：你可在此时放入一些初始物资，调整网络初始容量...
     */
    public static class Created extends DimensionsNetEvent
    {
        public Created(DimensionsNet net)
        {
            super(net);
        }
    }

    /**
     * 维度网络销毁时的事件，当此事件发送时，维度网络已被销毁
     * <p>
     * 用途：你可以在此时为玩家补偿一些物资...
     */
    public static class Destroyed extends Event
    {
        /**
         * 被摧毁网络的原始id，后续无法再通过此id获取此网络
         */
        private final int destroyedId;

        /**
         * 被摧毁网络的自定义名，如果未自定义则为空字符串，不会为null
         */
        private final String netName;

        /**
         * 被摧毁网络的存储内容
         */
        private final List<KeyAmount> destroyedStorage;

        /**
         * 被摧毁网络的所有者
         */
        private final UUID owner;

        /**
         * 被摧毁网络的管理员，包含所有者
         */
        private final Set<UUID> managers;

        /**
         * 被摧毁网络的成员，包含管理者和所有者
         */
        private final Set<UUID> members;

        public Destroyed(int destroyedId, String netName, List<KeyAmount> destroyedStorage, UUID owner, Set<UUID> managers, Set<UUID> members)
        {
            this.destroyedId = destroyedId;
            this.netName = netName;
            this.destroyedStorage = destroyedStorage;
            this.owner = owner;
            this.managers = managers;
            this.members = members;
        }

        public int getDestroyedId()
        {
            return destroyedId;
        }

        public String getNetName()
        {
            return netName;
        }

        public List<KeyAmount> getDestroyedStorage()
        {
            return destroyedStorage;
        }

        public UUID getOwner()
        {
            return owner;
        }

        public Set<UUID> getManagers()
        {
            return managers;
        }

        public Set<UUID> getMembers()
        {
            return members;
        }
    }

    /**
     * 表述网络所有权变更的事件
     * <p>
     * 仅表述同一个网络内的变更，网络合并的本质是销毁，应检查{@link Destroyed}事件
     */
    public static class OwnerChanged extends DimensionsNetEvent
    {
        private final UUID beforeOwner;
        private final UUID afterOwner;

        public OwnerChanged(DimensionsNet net, UUID beforeOwner, UUID afterOwner)
        {
            super(net);
            this.beforeOwner = beforeOwner;
            this.afterOwner = afterOwner;
        }

        public UUID getBeforeOwner()
        {
            return beforeOwner;
        }

        public UUID getAfterOwner()
        {
            return afterOwner;
        }
    }

    /**
     * 用于表述非所有权变更的状态
     */
    public enum MemberChangeState
    {
        // 加入、退出、被踢出
        JOINED_AS_MEMBER,
        JOINED_AS_MANAGER,
        LEFT_AS_MEMBER,
        LEFT_AS_MANAGER,
        KICKED_AS_MEMBER,
        KICKED_AS_MANAGER,

        // 权限变更
        PROMOTED_AS_MANAGER,
        DEMOTED_AS_MEMBER,
    }

    /**
     * 表述非所有权变更的事件
     */
    public static class MemberChanged extends DimensionsNetEvent
    {
        private final UUID changedPlayer;
        private final MemberChangeState changeState;

        public MemberChanged(DimensionsNet net, UUID changedPlayer, MemberChangeState changeState)
        {
            super(net);
            this.changedPlayer = changedPlayer;
            this.changeState = changeState;
        }

        public UUID getChangedPlayer()
        {
            return changedPlayer;
        }

        public MemberChangeState getChangeState()
        {
            return changeState;
        }
    }
}
