package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class NetedBlockEntity extends BlockEntity
{
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
        boolean needsUpdate = this.netId != id;
        this.netId = id;

        // 标识符变化时更新缓存
        if (level != null)
        {
            if (needsUpdate)
            {
                refreshNetCache();
                setChanged();
            }
        }
    }

    public void clearNetId()
    {
        boolean needsUpdate = this.netId != -1;
        this.netId = -1;
        net = null; // 清空缓存
        if (level != null)
        {
            if (needsUpdate)
            {
                setChanged();
            }
        }
    }

    public void setNetIdFromPlayer(ServerPlayer player)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            setNetId(net.getId());
        }
    }

    public void setNetIdFromPlayerOrClean(ServerPlayer player)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
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
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        setNetId(input.getIntOr("net_id", input.getIntOr("netId", -1)));
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        output.putInt("net_id", this.netId);
    }

    @Override
    public void onLoad()
    {
        // 完成读取后刷新一次缓存
        super.onLoad();
        refreshNetCache();
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries)
    {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected HolderLookup.Provider lookupProvider()
    {
        if (this.level != null)
        {
            return this.level.registryAccess();
        }

        if (ServerLifecycleHooks.getCurrentServer() != null)
        {
            return ServerLifecycleHooks.getCurrentServer().registryAccess();
        }

        throw new IllegalStateException("No registry lookup provider available for block entity serialization");
    }
}
