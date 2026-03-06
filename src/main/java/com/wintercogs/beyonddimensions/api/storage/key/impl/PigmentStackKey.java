package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.ChemicalStackKeyRender;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * 1.20.1: Key = pigment
 * - 不可变
 * - Key 层不带数量
 * - read-only/render 返回 amount=1 的缓存
 * - serialize/serializeNBT 只写 payload（typeId 由 common 方法包一层）
 */
public final class PigmentStackKey implements IStackKey<PigmentStack>
{
    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BDConstants.MODID, "stack_type/chemicals/pigment");

    public static final PigmentStackKey EMPTY = new PigmentStackKey(MekanismAPI.EMPTY_PIGMENT);

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    // ===== 不可变要素 =====
    private final Pigment pigment;

    // ===== 缓存（amount 恒为 1）=====
    private transient PigmentStack serverCache;
    private transient PigmentStack clientCache;

    private transient int hashCache;
    private transient boolean hashReady;

    private PigmentStackKey(Pigment pigment)
    {
        this.pigment = (pigment == null) ? MekanismAPI.EMPTY_PIGMENT : pigment;
    }

    public PigmentStackKey(PigmentStack stack)
    {
        this(stack == null ? MekanismAPI.EMPTY_PIGMENT : stack.getType());
    }

    // ---------------- IStackKey ----------------

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof PigmentStack s)
        {
            return new KeyAmount(new PigmentStackKey(s), s.getAmount());
        }
        return null;
    }

    @Override
    public @Nullable IStackKey<PigmentStack> fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        // PigmentStack 没有额外 tag 语义；忽略 dataComponentPatch
        if (key instanceof Pigment p)
        {
            return new PigmentStackKey(p);
        }
        return null;
    }

    @Override
    public PigmentStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.pigment.isEmptyType() ? PigmentStack.EMPTY : new PigmentStack(this.pigment, 1);
        }

        if (this.pigment.isEmptyType())
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = PigmentStack.EMPTY;
            }
            return PigmentStack.EMPTY;
        }

        PigmentStack cache = this.serverCache;
        // 如果缓存被外界换了 type（理论不该发生，但保险），就重建
        if (cache.isEmpty() || cache.getType() != this.pigment)
        {
            this.serverCache = new PigmentStack(this.pigment, 1);
            return this.serverCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<PigmentStack> getStackClass()
    {
        return PigmentStack.class;
    }

    @Override
    public @NotNull Pigment getSource()
    {
        return pigment;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Pigment.class;
    }

    @Override
    public String getModId()
    {
        var key = MekanismAPI.pigmentRegistry().getKey(this.pigment);
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return this == EMPTY || this.pigment.isEmptyType();
    }

    @Override
    public IStackKey<PigmentStack> getEmpty()
    {
        return EMPTY;
    }

    @Override
    public PigmentStack getEmptyStack()
    {
        return PigmentStack.EMPTY;
    }

    @Override
    public PigmentStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public PigmentStack copyStackWithCount(long count)
    {
        if (this.pigment.isEmptyType() || count <= 0) return PigmentStack.EMPTY;
        return new PigmentStack(this.pigment, count);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 64_000L;
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        if (tagKey == null || this.pigment.isEmptyType()) return false;
        if (!tagKey.isFor(MekanismAPI.PIGMENT_REGISTRY_NAME)) return false;

        @SuppressWarnings("unchecked")
        TagKey<Pigment> pigmentTag = (TagKey<Pigment>) tagKey;
        return this.pigment.is(pigmentTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return this.pigment.getTags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof PigmentStackKey o)
        {
            return this.pigment == o.pigment;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        // PigmentStack 没有额外 NBT 语义，精确匹配就是 type 相等
        return isSame(other);
    }

    /**
     * 网络序列化：只写 payload（typeId 由 IStackKey.serializeCommon 写）
     */
    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        boolean hasType = !this.pigment.isEmptyType();
        buf.writeBoolean(hasType);
        if (!hasType) return;
        buf.writeRegistryId(MekanismAPI.pigmentRegistry(), this.pigment);
    }

    @Override
    public @NotNull IStackKey<PigmentStack> deserialize(FriendlyByteBuf buf)
    {
        boolean hasType = buf.readBoolean();
        if (!hasType) return EMPTY;

        Pigment p = buf.readRegistryIdSafe(Pigment.class);
        if (p == null || p.isEmptyType()) return EMPTY;

        return new PigmentStackKey(p);
    }

    /**
     * NBT 序列化：只写 payload（外层由 serializeNBTCommon 写 type）
     */
    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();
        ResourceLocation id = MekanismAPI.pigmentRegistry().getKey(this.pigment);
        out.putString("pigment", id == null ? "mekanism:empty_pigment" : id.toString());
        return out;
    }

    @Override
    public @NotNull IStackKey<PigmentStack> deserializeNBT(CompoundTag nbt)
    {
        if (nbt == null) return EMPTY;

        // 旧
        if (nbt.contains("Stack", net.minecraft.nbt.Tag.TAG_COMPOUND))
        {
            return fromLegacyTypedStack(nbt.getCompound("Stack"));
        }
        // 新
        return readNewFmt(nbt);
    }

    private @NotNull IStackKey<PigmentStack> readNewFmt(@NotNull CompoundTag nbt)
    {
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("pigment"));
        Pigment p = (id == null) ? MekanismAPI.EMPTY_PIGMENT : MekanismAPI.pigmentRegistry().getValue(id);
        if (p == null) p = MekanismAPI.EMPTY_PIGMENT;
        return new PigmentStackKey(p);
    }

    private @NotNull IStackKey<PigmentStack> fromLegacyTypedStack(@NotNull CompoundTag stackNbt)
    {
        try
        {
            PigmentStack ps = PigmentStack.readFromNBT(stackNbt);
            if (ps.isEmpty())
            {
                return EMPTY;
            }

            Pigment p = ps.getType();
            return new PigmentStackKey(p);
        }
        catch (Throwable t)
        {
            return EMPTY;
        }
    }


    @Override
    public @NotNull IStackRender getRender()
    {
        return ChemicalStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull PigmentStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.pigment.isEmptyType() ? PigmentStack.EMPTY : new PigmentStack(this.pigment, 1);
        }

        if (this.pigment.isEmptyType())
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = PigmentStack.EMPTY;
            }
            return PigmentStack.EMPTY;
        }

        PigmentStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getType() != this.pigment)
        {
            this.clientCache = new PigmentStack(this.pigment, 1);
            return this.clientCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof PigmentStackKey o) return this.pigment == o.pigment;
        return false;
    }

    @Override
    public int hashCode()
    {
        if (!hashReady)
        {
            hashCache = 31 + pigment.hashCode();
            hashReady = true;
        }
        return hashCache;
    }
}