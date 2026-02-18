package com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackRender;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * 1.20.1: Key = infuse_type
 * - 不可变
 * - Key 层不带数量
 * - read-only/render 返回 amount=1 的缓存
 * - serialize/serializeNBT 只写 payload（typeId 由 common 方法包一层）
 */
public final class InfusionStackKey implements IStackKey<InfusionStack>
{
    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/chemicals/infuse");

    public static final InfusionStackKey EMPTY = new InfusionStackKey(MekanismAPI.EMPTY_INFUSE_TYPE);

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    // ===== 不可变要素 =====
    private final InfuseType infuseType;

    // ===== 缓存（amount 恒为 1）=====
    private transient InfusionStack serverCache;
    private transient InfusionStack clientCache;

    private transient int hashCache;
    private transient boolean hashReady;

    private InfusionStackKey(InfuseType infuseType)
    {
        this.infuseType = (infuseType == null) ? MekanismAPI.EMPTY_INFUSE_TYPE : infuseType;
    }

    public InfusionStackKey(InfusionStack stack)
    {
        this(stack == null ? MekanismAPI.EMPTY_INFUSE_TYPE : stack.getType());
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
        if (stack instanceof InfusionStack s)
        {
            return new KeyAmount(new InfusionStackKey(s), s.getAmount());
        }
        return null;
    }

    @Override
    public @Nullable IStackKey<InfusionStack> fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        // InfusionStack 没有额外 tag 语义；忽略 dataComponentPatch
        if (key instanceof InfuseType t)
        {
            return new InfusionStackKey(t);
        }
        return null;
    }

    @Override
    public InfusionStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.infuseType.isEmptyType() ? InfusionStack.EMPTY : new InfusionStack(this.infuseType, 1);
        }

        if (this.infuseType.isEmptyType())
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = InfusionStack.EMPTY;
            }
            return InfusionStack.EMPTY;
        }

        InfusionStack cache = this.serverCache;
        // 如果缓存被外界换了 type（理论不该发生，但保险），就重建
        if (cache.isEmpty() || cache.getType() != this.infuseType)
        {
            this.serverCache = new InfusionStack(this.infuseType, 1);
            return this.serverCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<InfusionStack> getStackClass()
    {
        return InfusionStack.class;
    }

    @Override
    public @NotNull InfuseType getSource()
    {
        return infuseType;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return InfuseType.class;
    }

    @Override
    public String getModId()
    {
        var key = MekanismAPI.infuseTypeRegistry().getKey(this.infuseType);
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return this == EMPTY || this.infuseType.isEmptyType();
    }

    @Override
    public IStackKey<InfusionStack> getEmpty()
    {
        return EMPTY;
    }

    @Override
    public InfusionStack getEmptyStack()
    {
        return InfusionStack.EMPTY;
    }

    @Override
    public InfusionStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public InfusionStack copyStackWithCount(long count)
    {
        if (this.infuseType.isEmptyType() || count <= 0) return InfusionStack.EMPTY;
        return new InfusionStack(this.infuseType, count);
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
        if (tagKey == null || this.infuseType.isEmptyType()) return false;
        if (!tagKey.isFor(MekanismAPI.INFUSE_TYPE_REGISTRY_NAME)) return false;

        @SuppressWarnings("unchecked")
        TagKey<InfuseType> infuseTag = (TagKey<InfuseType>) tagKey;
        return this.infuseType.is(infuseTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return this.infuseType.getTags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof InfusionStackKey o)
        {
            return this.infuseType == o.infuseType;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        // InfusionStack 没有额外 NBT 语义，精确匹配就是 type 相等
        return isSame(other);
    }

    /**
     * 网络序列化：只写 payload（typeId 由 IStackKey.serializeCommon 写）
     */
    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        boolean hasType = !this.infuseType.isEmptyType();
        buf.writeBoolean(hasType);
        if (!hasType) return;
        buf.writeRegistryId(MekanismAPI.infuseTypeRegistry(), this.infuseType);
    }

    @Override
    public @NotNull IStackKey<InfusionStack> deserialize(FriendlyByteBuf buf)
    {
        boolean hasType = buf.readBoolean();
        if (!hasType) return EMPTY;

        InfuseType t = buf.readRegistryIdSafe(InfuseType.class);
        if (t == null || t.isEmptyType()) return EMPTY;

        return new InfusionStackKey(t);
    }

    /**
     * NBT 序列化：只写 payload（外层由 serializeNBTCommon 写 type）
     */
    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();
        ResourceLocation id = MekanismAPI.infuseTypeRegistry().getKey(this.infuseType);
        out.putString("infuse_type", id == null ? "mekanism:empty_infuse_type" : id.toString());
        return out;
    }

    @Override
    public @NotNull IStackKey<InfusionStack> deserializeNBT(CompoundTag nbt)
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

    private @NotNull IStackKey<InfusionStack> readNewFmt(@NotNull CompoundTag nbt)
    {
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("infuse_type"));
        InfuseType t = (id == null) ? MekanismAPI.EMPTY_INFUSE_TYPE : MekanismAPI.infuseTypeRegistry().getValue(id);
        if (t == null) t = MekanismAPI.EMPTY_INFUSE_TYPE;
        return new InfusionStackKey(t);
    }

    private @NotNull IStackKey<InfusionStack> fromLegacyTypedStack(@NotNull CompoundTag stackNbt)
    {
        try
        {
            InfusionStack is = InfusionStack.readFromNBT(stackNbt);
            if (is.isEmpty())
            {
                return EMPTY;
            }

            InfuseType t = is.getType();
            return new InfusionStackKey(t);
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
    public @NotNull InfusionStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.infuseType.isEmptyType() ? InfusionStack.EMPTY : new InfusionStack(this.infuseType, 1);
        }

        if (this.infuseType.isEmptyType())
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = InfusionStack.EMPTY;
            }
            return InfusionStack.EMPTY;
        }

        InfusionStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getType() != this.infuseType)
        {
            this.clientCache = new InfusionStack(this.infuseType, 1);
            return this.clientCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof InfusionStackKey o) return this.infuseType == o.infuseType;
        return false;
    }

    @Override
    public int hashCode()
    {
        if (!hashReady)
        {
            hashCache = 31 + infuseType.hashCode();
            hashReady = true;
        }
        return hashCache;
    }
}
