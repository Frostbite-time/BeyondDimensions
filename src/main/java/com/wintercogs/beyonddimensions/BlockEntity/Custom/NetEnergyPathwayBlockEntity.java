package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Storage.EnergyStorage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;


public class NetEnergyPathwayBlockEntity extends NetedBlockEntity implements ITickable
{

    // 配置参数
    public final int transHold = 20;
    public int transTime = 0;
    public boolean popMode = false;
    // 方向缓存 (1.12.2 使用 EnumFacing)
    private final EnumFacing[] directions = EnumFacing.values();

    // 能量存储缓存
    private EnergyStorage energyStorageCache;
    public NetEnergyPathwayBlockEntity() {
        super();
    }
    // 1.12.2 Tick 方法
    @Override
    public void update() {
        if (world.isRemote) return; // 客户端不执行
        // 网络状态检查
        if (getNetId() == -1) return;
        transTime++;
        if (transTime >= transHold) {
            transTime = 0;
            // 执行周期性操作
        }
        // 能量输出模式
        if (popMode) {
            popEnergy();
        }
    }
    // 能量输出逻辑
    private void popEnergy() {
        if (energyStorageCache == null) {
            DimensionsNet net = getNet();
            if (net == null) return;
            energyStorageCache = net.getEnergyStorage();
        }
        for (EnumFacing dir : directions) {
            BlockPos targetPos = getPos().offset(dir);
            TileEntity neighbor = world.getTileEntity(targetPos);
            if (neighbor == null || neighbor instanceof NetedBlockEntity) continue;
            // 获取相邻方块能力 (1.12.2 无 LazyOptional)
            IEnergyStorage otherStorage = neighbor.getCapability(
                    CapabilityEnergy.ENERGY,
                    dir.getOpposite()
            );
            if (otherStorage != null) {
                int maxExtract = (int) Math.min(
                        energyStorageCache.getEnergyStored(),
                        energyStorageCache.getMaxTransfer()
                );
                int received = otherStorage.receiveEnergy(maxExtract, false);
                energyStorageCache.extractEnergy(received, false);
            }
        }
    }
    // 能力提供 (1.12.2 直接返回实例)
    @Override
    public <T> T getCapability(Capability<T> cap, EnumFacing side) {
        if (cap == CapabilityEnergy.ENERGY) {
            DimensionsNet net = getNet();
            if (net != null) {
                return CapabilityEnergy.ENERGY.cast(net.getEnergyStorage());
            }
        }
        return super.getCapability(cap, side);
    }
    // 数据保存
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("popMode", popMode);
        return compound;
    }
    // 数据加载
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        popMode = compound.getBoolean("popMode");
    }
    // 区块卸载时清理缓存
    @Override
    public void onChunkUnload() {
        energyStorageCache = null;
    }

}
