package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.LongType.EnergyType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class EnergyStackType extends LongStackType<EnergyType>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/energy");

    public EnergyStackType()
    {
        stack = new EnergyType(0);
    }

    public EnergyStackType(EnergyType stack)
    {
        this.stack = stack;
    }

    public EnergyStackType(long stackSize)
    {
        this.stack = new EnergyType(stackSize);
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public IStackType<EnergyType> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof EnergyType)
        {
            return new EnergyStackType(amount);
        }
        return null;
    }

    @Override
    public IStackType<EnergyType> getEmpty()
    {
        return new EnergyStackType();
    }

    @Override
    public Object getSource()
    {
        return new EnergyType(0);
    }

    @Override
    public EnergyType getEmptyStack()
    {
        return new EnergyType(0);
    }

    @Override
    public IStackType<EnergyType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        EnergyStackType copy = new EnergyStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<EnergyType> copyWithCount(long count)
    {
        EnergyStackType copy = new EnergyStackType(count);
        if(count == stack.getStackCount())
        {
            copy.NeedRecalHash = this.NeedRecalHash;
            copy.hashCodeCache = this.hashCodeCache;
        }
        return copy;
    }

    @Override
    public IStackType<EnergyType> split(long amount)
    {
        if (amount <= 0) return new EnergyStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new EnergyStackType(splitAmount);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId()); // 会被deserializeCommon读取，因此deserialize中不要读取它
        // 写入数量
        buf.writeVarLong(stack.getStackCount());
    }

    @Override
    public IStackType<EnergyType> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new EnergyStackType(count);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        return tag;
    }

    @Override
    public IStackType<EnergyType> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        return new EnergyStackType(nbt.getLong("Amount"));
    }
}
