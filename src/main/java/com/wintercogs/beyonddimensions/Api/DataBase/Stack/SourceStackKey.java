package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Api.DataBase.LongType.SourceType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class SourceStackKey extends LongStackKey<SourceType>
{

    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/source");

    /**
     * 唯一实例
     */
    public static final SourceStackKey INSTANCE = new SourceStackKey();

    private SourceStackKey()
    {
        this.stack = new SourceType(0);
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof SourceType sourceType)
            return new KeyAmount(SourceStackKey.INSTANCE, sourceType.getStackCount());
        return null;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 10000;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.ARS_ModId;
    }

    @Override
    public SourceStackKey getEmpty()
    {
        return SourceStackKey.INSTANCE;
    }

    /**
     * 允许从 SourceType 或 Number（数量无意义）映射为同一个 Key 实例
     */
    @Override
    public @Nullable SourceStackKey fromSourceObject(Object key, CompoundTag ignored)
    {
        if (key instanceof SourceType || key instanceof Number) return INSTANCE;
        return null;
    }

    @Override
    public @NotNull SourceType getSource()
    {
        return this.stack;
    }

    @Override
    public SourceType getEmptyStack()
    {
        return new SourceType(0);
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
    public @NotNull SourceStackKey deserialize(FriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull SourceStackKey deserializeNBT(CompoundTag nbt)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return SourceStackKeyRender.INSTANCE;
    }
}