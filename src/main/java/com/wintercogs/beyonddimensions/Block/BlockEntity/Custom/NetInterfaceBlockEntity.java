package com.wintercogs.beyonddimensions.Block.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Block.BlockEntity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class NetInterfaceBlockEntity extends BlockEntity
{
    // 存储接口所对应的维度网络id，用于和维度网络交互
    private int netId = -1; // 初始化为-1，表示未绑定

    public NetInterfaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public int getNetId()
    {
        return this.netId;
    }

    public void setNetId(int netId)
    {
        this.netId = netId;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.netId = tag.getInt("netId");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putInt("netId",this.netId);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag,registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider)
    {
        super.handleUpdateTag(tag, lookupProvider);
    }

    @Override // 使用此数据包进行同步
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 在接受更新数据包时做一些自定义操作
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider)
    {
        super.onDataPacket(net, pkt, lookupProvider);
    }
}
