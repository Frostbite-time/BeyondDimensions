package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.ChemicalStackKeyRender;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * 1.20.1: Key = slurry
 * - 不可变
 * - Key 层不带数量
 * - read-only/render 返回 amount=1 的缓存
 * - serialize/serializeNBT 只写 payload（typeId 由 common 方法包一层）
 */
public final class SlurryStackKey implements IStackKey<SlurryStack>
{
    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BDConstants.MODID, "stack_type/chemicals/slurry");

    public static final SlurryStackKey EMPTY = new SlurryStackKey(MekanismAPI.EMPTY_SLURRY);

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    // ===== 不可变要素 =====
    private final Slurry slurry;

    // ===== 缓存（amount 恒为 1）=====
    private transient SlurryStack serverCache;
    private transient SlurryStack clientCache;

    private transient int hashCache;
    private transient boolean hashReady;

    private SlurryStackKey(Slurry slurry)
    {
        this.slurry = (slurry == null) ? MekanismAPI.EMPTY_SLURRY : slurry;
    }

    public SlurryStackKey(SlurryStack stack)
    {
        this(stack == null ? MekanismAPI.EMPTY_SLURRY : stack.getType());
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
        if (stack instanceof SlurryStack s)
        {
            return new KeyAmount(new SlurryStackKey(s), s.getAmount());
        }
        return null;
    }

    @Override
    public @Nullable IStackKey<SlurryStack> fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        // SlurryStack 没有额外 tag 语义；忽略 dataComponentPatch
        if (key instanceof Slurry s)
        {
            return new SlurryStackKey(s);
        }
        return null;
    }

    @Override
    public SlurryStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.slurry.isEmptyType() ? SlurryStack.EMPTY : new SlurryStack(this.slurry, 1);
        }

        if (this.slurry.isEmptyType())
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = SlurryStack.EMPTY;
            }
            return SlurryStack.EMPTY;
        }

        SlurryStack cache = this.serverCache;
        // 如果缓存被外界换了 type（理论不该发生，但保险），就重建
        if (cache.isEmpty() || cache.getType() != this.slurry)
        {
            this.serverCache = new SlurryStack(this.slurry, 1);
            return this.serverCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<SlurryStack> getStackClass()
    {
        return SlurryStack.class;
    }

    @Override
    public @NotNull Slurry getSource()
    {
        return slurry;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Slurry.class;
    }

    @Override
    public String getModId()
    {
        var key = MekanismAPI.slurryRegistry().getKey(this.slurry);
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return this == EMPTY || this.slurry.isEmptyType();
    }

    @Override
    public IStackKey<SlurryStack> getEmpty()
    {
        return EMPTY;
    }

    @Override
    public SlurryStack getEmptyStack()
    {
        return SlurryStack.EMPTY;
    }

    @Override
    public SlurryStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public SlurryStack copyStackWithCount(long count)
    {
        if (this.slurry.isEmptyType() || count <= 0) return SlurryStack.EMPTY;
        return new SlurryStack(this.slurry, count);
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
        if (tagKey == null || this.slurry.isEmptyType()) return false;
        if (!tagKey.isFor(MekanismAPI.SLURRY_REGISTRY_NAME)) return false;

        @SuppressWarnings("unchecked")
        TagKey<Slurry> slurryTag = (TagKey<Slurry>) tagKey;
        return this.slurry.is(slurryTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return this.slurry.getTags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof SlurryStackKey o)
        {
            return this.slurry == o.slurry;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        // SlurryStack 没有额外 NBT 语义，精确匹配就是 type 相等
        return isSame(other);
    }

    /**
     * 网络序列化：只写 payload（typeId 由 IStackKey.serializeCommon 写）
     */
    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        boolean hasType = !this.slurry.isEmptyType();
        buf.writeBoolean(hasType);
        if (!hasType) return;

        buf.writeRegistryId(MekanismAPI.slurryRegistry(), this.slurry);
    }

    @Override
    public @NotNull IStackKey<SlurryStack> deserialize(FriendlyByteBuf buf)
    {
        boolean hasType = buf.readBoolean();
        if (!hasType) return EMPTY;

        Slurry s = buf.readRegistryIdSafe(Slurry.class);
        if (s == null || s.isEmptyType()) return EMPTY;

        return new SlurryStackKey(s);
    }

    /**
     * NBT 序列化：只写 payload（外层由 serializeNBTCommon 写 type）
     */
    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();
        ResourceLocation id = MekanismAPI.slurryRegistry().getKey(this.slurry);
        out.putString("slurry", id == null ? "mekanism:empty_slurry" : id.toString());
        return out;
    }

    @Override
    public @NotNull IStackKey<SlurryStack> deserializeNBT(CompoundTag nbt)
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

    private @NotNull IStackKey<SlurryStack> readNewFmt(@NotNull CompoundTag nbt)
    {
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("slurry"));
        Slurry s = (id == null) ? MekanismAPI.EMPTY_SLURRY : MekanismAPI.slurryRegistry().getValue(id);
        if (s == null) s = MekanismAPI.EMPTY_SLURRY;
        return new SlurryStackKey(s);
    }

    private @NotNull IStackKey<SlurryStack> fromLegacyTypedStack(@NotNull CompoundTag stackNbt)
    {
        try
        {
            SlurryStack ss = SlurryStack.readFromNBT(stackNbt);
            if (ss.isEmpty())
            {
                return EMPTY;
            }

            Slurry s = ss.getType();
            return new SlurryStackKey(s);
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
    public @NotNull SlurryStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.slurry.isEmptyType() ? SlurryStack.EMPTY : new SlurryStack(this.slurry, 1);
        }

        if (this.slurry.isEmptyType())
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = SlurryStack.EMPTY;
            }
            return SlurryStack.EMPTY;
        }

        SlurryStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getType() != this.slurry)
        {
            this.clientCache = new SlurryStack(this.slurry, 1);
            return this.clientCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof SlurryStackKey o) return this.slurry == o.slurry;
        return false;
    }

    @Override
    public int hashCode()
    {
        if (!hashReady)
        {
            hashCache = 31 + slurry.hashCode();
            hashReady = true;
        }
        return hashCache;
    }
}