package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.ManaType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class ManaStackType extends LongStackType<ManaType>
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/mana");
    public static final ManaStackType EMPTY = new ManaStackType(); // 空定义

    public static final MapCodec<ManaStackType> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ManaType.CODEC.fieldOf("internal_stack").forGetter(ManaStackType::getStack)
            ).apply(instance, ManaStackType::new));

    public static final Codec<ManaStackType> CODEC = TYPE_CODEC.codec();

    public ManaStackType()
    {
        stack = new ManaType(0);
    }

    public ManaStackType(ManaType stack)
    {
        this.stack = stack;
    }

    public ManaStackType(long stackSize)
    {
        this.stack = new ManaType(stackSize);
    }


    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public MapCodec<? extends IStackType<ManaType>> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public IStackType<ManaType> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof ManaType)
        {
            return new ManaStackType(amount);
        }
        return null;
    }

    @Override
    public IStackType<ManaType> getEmpty()
    {
        return new ManaStackType(0);
    }

    @Override
    public Object getSource()
    {
        return new ManaType(0);
    }

    @Override
    public ManaType getEmptyStack()
    {
        return new ManaType(0);
    }

    @Override
    public IStackType<ManaType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        ManaStackType copy = new ManaStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<ManaType> copyWithCount(long count)
    {
        ManaStackType copy = new ManaStackType(count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<ManaType> split(long amount)
    {
        if (amount <= 0) return new ManaStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new ManaStackType(splitAmount);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 1000000L; // 一格一个池子，很合理~
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
    public IStackType<ManaType> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new ManaStackType(count);
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
    public IStackType<ManaType> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        return new ManaStackType(nbt.getLong("Amount"));
    }

}
