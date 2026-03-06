package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.longtype.ManaType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.ManaStackKeyRender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class ManaStackKey extends LongStackKey<ManaType>
{

    public static final ResourceLocation ID = ResourceLocation.tryBuild(BDConstants.MODID, "stack_type/mana");

    /**
     * 唯一实例
     */
    public static final ManaStackKey INSTANCE = new ManaStackKey();

    private ManaStackKey()
    {
        this.stack = new ManaType(0);
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof ManaType manaType)
            return new KeyAmount(ManaStackKey.INSTANCE, manaType.getStackCount());
        return null;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.Botania_ModId;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 1000000;
    }

    @Override
    public ManaStackKey getEmpty()
    {
        return ManaStackKey.INSTANCE;
    }

    /**
     * 允许从 ManaType 或 Number（数量无意义）映射到同一个 Key 实例
     */
    @Override
    public @Nullable ManaStackKey fromSourceObject(Object key, CompoundTag ignored)
    {
        if (key instanceof ManaType || key instanceof Number)
        {
            return INSTANCE;
        }
        return null;
    }

    @Override
    public @NotNull ManaType getSource()
    {
        return this.stack;
    }

    @Override
    public ManaType getEmptyStack()
    {
        return new ManaType(0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return Stream.empty();
    }

    @Override
    public void serialize(FriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull ManaStackKey deserialize(FriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull ManaStackKey deserializeNBT(CompoundTag nbt)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return ManaStackKeyRender.INSTANCE;
    }
}