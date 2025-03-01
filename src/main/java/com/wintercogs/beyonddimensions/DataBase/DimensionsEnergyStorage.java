package com.wintercogs.beyonddimensions.DataBase;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class DimensionsEnergyStorage implements IEnergyStorage
{
    private DimensionsNet net; // 用于通知维度网络进行保存
    // 实际的存储
    private long energyStorage;
    private final long capacity = Long.MAX_VALUE-1;
    private final int maxTransfer = Integer.MAX_VALUE-1;

    public DimensionsEnergyStorage()
    {
        this.energyStorage = 0;
    }

    public DimensionsEnergyStorage(DimensionsNet net)
    {
        this.net = net;
        this.energyStorage = 0;
    }

    // 自定义方法访问实际 long 值（供内部逻辑使用）
    public long getRealEnergyStored() {
        return energyStorage;
    }

    public void setEnergyDirectly(long value) {
        this.energyStorage = Math.max(0, Math.min(value, capacity));
    }

    private void OnChange()
    {
        net.setDirty();
    }

    // 将物品存储转换为 NBT 数据
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
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
