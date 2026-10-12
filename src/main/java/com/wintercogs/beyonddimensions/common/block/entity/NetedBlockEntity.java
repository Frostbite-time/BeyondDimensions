package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.event.dimensionnet.NetedBlockEvent;
import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class NetedBlockEntity extends BlockEntity
{
    private static final String NET_ID = "netId";

    // 存储接口所对应的维度网络id，用于和维度网络交互
    private int netId = -1;// 初始化为-1，任何小于0（不包括0）的id表示未绑定网络
    private DimensionsNet net = null; //缓存net
    private List<Runnable> onNetChangeRunnables = new ArrayList<>();

    public NetedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState)
    {
        super(type, pos, blockState);
    }

    public int getNetId()
    {
        return netId;
    }

    public void setNetId(int id)
    {
        if (this.netId == id)
            return;

        int previousNetId = this.netId;
        this.netId = id;

        // 标识符变化时更新缓存
        if (level != null)
        {
            refreshNetCache();
            setChanged();
            syncToClient();

            if (level instanceof ServerLevel serverLevel && getBlockState().getBlock() instanceof NetedBlock block)
            {
                if (previousNetId >= 0)
                {
                    NeoForge.EVENT_BUS.post(new NetedBlockEvent.Unbound(
                            previousNetId, serverLevel, worldPosition, getBlockState(), block, this
                    ));
                }
                if (id >= 0)
                {
                    NeoForge.EVENT_BUS.post(new NetedBlockEvent.Bound(
                            id, serverLevel, worldPosition, getBlockState(), block, this
                    ));
                }
            }
        }
    }

    public void clearNetId()
    {
        setNetId(-1);
    }

    public void setNetIdFromPlayer(ServerPlayer player)
    {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net != null)
        {
            setNetId(net.getId());
        }
    }

    public void setNetIdFromPlayerOrClean(ServerPlayer player)
    {
        DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net != null)
        {
            setNetId(net.getId());
        }
        else
        {
            clearNetId();
        }
    }

    public DimensionsNet getNet()
    {
        if (netId >= 0) // netId作为方块网络的第一标识符高于缓存
        {
            if (net == null || net.deleted)
            {
                // 当缓存无效时尝试更正
                refreshNetCache();
            }
            return net; // 返回最终缓存
        }
        else
        {
            return null;
        }
    }

    // 更新网络缓存
    protected void refreshNetCache()
    {
        if (getLevel() instanceof ServerLevel)
        {
            DimensionsNet netCache = DimensionsNet.getNetFromId(netId);
            if (netCache != null && !netCache.deleted)
            {
                net = netCache;
            }
            else
            {
                net = null;
            }
        }
    }

    public void addNetChangeTask(Runnable runnable)
    {
        onNetChangeRunnables.add(runnable);
    }

    public void onNetChange()
    {
        for (Runnable runnable : onNetChangeRunnables)
            runnable.run();
    }

    @Override
    public void setChanged()
    {
        super.setChanged();
        onNetChange();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        this.netId = tag.getInt(NET_ID);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt(NET_ID, this.netId);
    }

    @Override
    public void onLoad()
    {
        // 完成读取后刷新一次缓存
        super.onLoad();
        refreshNetCache();
    }

    /**
     * 区块包与方块更新包带给客户端的数据，只含 {@link #saveClientData} 写出的字段：
     * 整份存档可能大到让区块包超过协议上限
     */
    @Override
    public final @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        saveClientData(tag, registries);
        return tag;
    }

    @Override
    public final @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 在客户端执行：随区块包收到 {@link #getUpdateTag} 的数据
     */
    @Override
    public final void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries)
    {
        loadClientData(tag, registries);
    }

    /**
     * 在客户端执行：收到方块更新包
     */
    @Override
    public final void onDataPacket(@NotNull Connection connection, @NotNull ClientboundBlockEntityDataPacket packet, HolderLookup.@NotNull Provider registries)
    {
        loadClientData(packet.getTag(), registries);
    }

    /**
     * 在服务端写出客户端需要读取的字段，区块包与方块更新包共用；子类追加时先调用 super
     */
    protected void saveClientData(CompoundTag tag, HolderLookup.Provider registries)
    {
        tag.putInt(NET_ID, netId);
    }

    /**
     * 在客户端读入 {@link #saveClientData} 写出的字段。收到的不是完整存档，字段可能缺失；子类追加时先调用 super
     */
    protected void loadClientData(CompoundTag tag, HolderLookup.Provider registries)
    {
        if (tag.contains(NET_ID, Tag.TAG_INT))
            this.netId = tag.getInt(NET_ID);
    }

    /**
     * 在服务端调用：{@link #saveClientData} 写出的数据改变后，把它发给附近的玩家
     */
    protected final void syncToClient()
    {
        if (level != null && !level.isClientSide())
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
