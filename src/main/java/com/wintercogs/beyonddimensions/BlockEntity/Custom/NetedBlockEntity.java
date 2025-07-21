package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class NetedBlockEntity extends BlockEntity
{
    // 存储接口所对应的维度网络id，用于和维度网络交互
    protected int netId = -1;// 初始化为-1，任何小于0（不包括0）的id表示未绑定网络
    private DimensionsNet net = null; //缓存net

    public NetedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public int getNetId()
    {
        return netId;
    }

    public void setNetId(int id)
    {
        this.netId = id;
        setChanged();
    }

    public void clearNetId()
    {
        this.netId = -1;
        setChanged();
    }

    public void setNetIdFromPlayer(ServerPlayer player)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if(net != null)
        {
            this.netId = net.getId();
            setChanged();
        }
    }

    public void setNetIdFromPlayerOrClean(ServerPlayer player)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if(net != null)
        {
            this.netId = net.getId();
            setChanged();
        }
        else
        {
            this.netId = -1;
            setChanged();
        }
    }

    public DimensionsNet getNet()
    {
        if(net == null || net.deleted)
        {
            if(netId>=0)
            {
                if(getLevel() instanceof ServerLevel)
                {
                    DimensionsNet netCache = DimensionsNet.getNetFromId(netId,getLevel().getServer());
                    if(netCache!=null && !netCache.deleted)
                    {
                        net = netCache;
                        return net;
                    }
                    return null;
                }
            }
            return null;
        }
        return net; // net正常，直接返回net
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.netId = tag.getInt("netId");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("netId",this.netId);
    }

    // 为子类提供基础的网络同步，需要正确实现loadAdditional和saveAdditional
    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
