package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EmptyStackKey implements IStackKey<EmptyStackKey.EmptyStackType>
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID,"stack_type/empty");
    public static final EmptyStackKey INSTANCE = new EmptyStackKey();

    private EmptyStackKey() {}

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public MapCodec<? extends EmptyStackKey> codec()
    {
        return null;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if(stack instanceof EmptyStackType)
            return new KeyAmount(INSTANCE,0);
        return null;
    }

    @Override
    public @Nullable EmptyStackKey fromSourceObject(Object key, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof EmptyStackType)
            return INSTANCE;
        return null;
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
        return BeyondDimensions.MODID;
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
    public void serialize(RegistryFriendlyByteBuf buf) {}

    @Override
    public @NotNull EmptyStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        return tag;
    }

    @Override
    public @NotNull EmptyStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
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

        private EmptyStackType() {}
    }
}
