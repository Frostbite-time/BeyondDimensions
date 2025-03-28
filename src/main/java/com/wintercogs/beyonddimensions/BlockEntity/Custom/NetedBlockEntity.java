package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;


public abstract class NetedBlockEntity extends TileEntity {
    protected int netId = -1;

    // 1.12.2 的 TileEntity 需要无参构造器
    public NetedBlockEntity() {}

    public int getNetId() {
        return netId;
    }

    public void setNetId(int id) {
        this.netId = id;
        markDirty(); // setChanged() → markDirty()
    }

    public void clearNetId() {
        this.netId = -1;
        markDirty();
    }

    public void setNetIdFromPlayer(EntityPlayerMP player) { // ServerPlayer → EntityPlayerMP
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null) {
            setNetId(net.getId());
        }
    }

    public void setNetIdFromPlayerOrClean(EntityPlayerMP player) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        setNetId(net != null ? net.getId() : -1);
    }

    public DimensionsNet getNet() {
        if (netId >= 0) {
            World world = getWorld(); // getLevel() → getWorld()
            if (world instanceof WorldServer) { // ServerLevel → WorldServer
                return DimensionsNet.getNetFromId(netId, (WorldServer) world);
            }
        }
        return null;
    }

    // 1.12.2 的 NBT 读写方法
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        netId = tag.getInteger("netId");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("netId", netId);
        return tag;
    }

    // 数据同步到客户端
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    // 客户端接收更新后的处理
    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
