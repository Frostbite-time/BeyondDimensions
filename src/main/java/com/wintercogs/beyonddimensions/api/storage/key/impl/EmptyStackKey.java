package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.EmptyStackKeyRender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public final class EmptyStackKey implements IStackKey<EmptyStackKey.EmptyStackType>
{

    public static final ResourceLocation ID = ResourceLocation.tryBuild(BDConstants.MODID, "stack_type/empty");
    public static final EmptyStackKey INSTANCE = new EmptyStackKey();

    private EmptyStackKey()
    {
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof EmptyStackType)
            return new KeyAmount(INSTANCE, 0);
        return null;
    }

    @Override
    public @Nullable EmptyStackKey fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        if (key instanceof EmptyStackType)
            return INSTANCE;
        return null;
    }

    @Override
    public EmptyStackType getReadOnlyStack()
    {
        return EmptyStackType.INSTANCE;
    }

    @Override
    public Class<EmptyStackType> getStackClass()
    {
        return EmptyStackType.class;
    }

    @Override
    public @NotNull Object getSource()
    {
        return EmptyStackType.INSTANCE;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return EmptyStackType.class;
    }

    @Override
    public String getModId()
    {
        return BDConstants.MODID;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public EmptyStackKey getEmpty()
    {
        return EmptyStackKey.INSTANCE;
    }

    @Override
    public EmptyStackType getEmptyStack()
    {
        return EmptyStackType.INSTANCE;
    }

    @Override
    public EmptyStackType copyStack()
    {
        return EmptyStackType.INSTANCE;
    }

    @Override
    public EmptyStackType copyStackWithCount(long count)
    {
        return EmptyStackType.INSTANCE;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 0;
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return 0;
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
    public boolean isSame(IStackKey<?> other)
    {
        return other instanceof EmptyStackKey;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        return other instanceof EmptyStackKey;
    }

    @Override
    public void serialize(FriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull EmptyStackKey deserialize(FriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull EmptyStackKey deserializeNBT(CompoundTag nbt)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return EmptyStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull EmptyStackKey.EmptyStackType getRenderStack()
    {
        return EmptyStackKey.EmptyStackType.INSTANCE;
    }


    public static class EmptyStackType
    {
        public static final EmptyStackType INSTANCE = new EmptyStackType();

        private EmptyStackType()
        {
        }
    }
}