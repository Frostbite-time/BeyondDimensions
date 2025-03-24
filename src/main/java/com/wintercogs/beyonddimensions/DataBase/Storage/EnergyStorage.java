package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorage implements IEnergyStorage
{
    private DimensionsNet net; // 用于通知维度网络进行保存
    // 实际的存储
    private long energyStorage;
    private final long capacity = Long.MAX_VALUE;
    private final int maxTransfer = Integer.MAX_VALUE;


    public EnergyStorage(DimensionsNet net)
    {
        this.net = net;
        this.energyStorage = 0;
    }

    // 自定义方法访问实际 long 值（供内部逻辑使用）
    public long getRealEnergyStored() {
        return energyStorage;
    }

    public long getRealEnergyCapacity()
    {
        return capacity;
    }

    public long getMaxTransfer()
    {
        return maxTransfer;
    }

    public void setEnergyDirectly(long value) {
        this.energyStorage = Math.max(0, Math.min(value, capacity));
    }

    private void OnChange()
    {
        net.setDirty();
    }

    // 将物品存储转换为 NBT 数据
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Energy",energyStorage);
        return tag;
    }

    // 从 NBT 数据加载物品存储
    public void deserializeNBT(HolderLookup.Provider levelRegistryAccess, CompoundTag tag)
    {

        if (tag.contains("Energy"))
        {
            energyStorage = tag.getLong("Energy");
        }
    }

    // 返回值为接受的能量总量
    @Override
    public int receiveEnergy(int amount, boolean simulate)
    {
        long accepted = Math.min(capacity - energyStorage, Math.min(maxTransfer, amount));
        if (!simulate) {
            energyStorage += accepted;
            OnChange();
        }
        return (int) accepted; // 安全转换，因为 maxTransfer 和 maxReceive 是 int
    }

    // 返回值为导出的能量总量
    @Override
    public int extractEnergy(int amount, boolean simulate)
    {
        long extracted = Math.min(energyStorage, Math.min(maxTransfer, amount));
        if (!simulate) {
            energyStorage -= extracted;
            OnChange();
        }
        return (int) extracted;
    }

    @Override
    public int getEnergyStored()
    {
        return (energyStorage > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) energyStorage;
    }

    @Override
    public int getMaxEnergyStored()
    {
        return Integer.MAX_VALUE-1;
    }

    @Override
    public boolean canExtract()
    {
        return true;
    }

    @Override
    public boolean canReceive()
    {
        return true;
    }
}
