package com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackRender;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * 1.20.1: Key = gas
 * - 不可变
 * - Key 层不带数量
 * - read-only/render 返回 amount=1 的缓存
 * - serialize/serializeNBT 只写 payload（typeId 由 common 方法包一层）
 */
public final class GasStackKey implements IStackKey<GasStack>
{
    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/chemicals/gas");

    public static final GasStackKey EMPTY = new GasStackKey(MekanismAPI.EMPTY_GAS);

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    // ===== 不可变要素 =====
    private final Gas gas;

    // ===== 缓存（amount 恒为 1）=====
    private transient GasStack serverCache;
    private transient GasStack clientCache;

    private transient int hashCache;
    private transient boolean hashReady;

    private GasStackKey(Gas gas)
    {
        this.gas = (gas == null) ? MekanismAPI.EMPTY_GAS : gas;
    }

    public GasStackKey(GasStack stack)
    {
        this(stack == null ? MekanismAPI.EMPTY_GAS : stack.getType());
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
        if (stack instanceof GasStack s)
        {
            return new KeyAmount(new GasStackKey(s), s.getAmount());
        }
        return null;
    }

    @Override
    public @Nullable IStackKey<GasStack> fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        // GasStack 没有额外 tag 语义；忽略 dataComponentPatch
        if (key instanceof Gas g)
        {
            return new GasStackKey(g);
        }
        return null;
    }

    @Override
    public GasStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.gas.isEmptyType() ? GasStack.EMPTY : new GasStack(this.gas, 1);
        }

        if (this.gas.isEmptyType())
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = GasStack.EMPTY;
            }
            return GasStack.EMPTY;
        }

        GasStack cache = this.serverCache;
        // 如果缓存被外界换了 type（理论不该发生，但保险），就重建
        if (cache.isEmpty() || cache.getType() != this.gas)
        {
            this.serverCache = new GasStack(this.gas, 1);
            return this.serverCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<GasStack> getStackClass()
    {
        return GasStack.class;
    }

    @Override
    public @NotNull Gas getSource()
    {
        return gas;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Gas.class;
    }

    @Override
    public String getModId()
    {
        var key = MekanismAPI.gasRegistry().getKey(this.gas);
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return this == EMPTY || this.gas.isEmptyType();
    }

    @Override
    public IStackKey<GasStack> getEmpty()
    {
        return EMPTY;
    }

    @Override
    public GasStack getEmptyStack()
    {
        return GasStack.EMPTY;
    }

    @Override
    public GasStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public GasStack copyStackWithCount(long count)
    {
        if (this.gas.isEmptyType() || count <= 0) return GasStack.EMPTY;
        return new GasStack(this.gas, count);
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
        if (tagKey == null || this.gas.isEmptyType()) return false;
        if (!tagKey.isFor(MekanismAPI.GAS_REGISTRY_NAME)) return false;

        @SuppressWarnings("unchecked")
        TagKey<Gas> gasTag = (TagKey<Gas>) tagKey;
        return this.gas.is(gasTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return this.gas.getTags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof GasStackKey o)
        {
            return this.gas == o.gas;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        // GasStack 没有额外 NBT 语义，精确匹配就是 type 相等
        return isSame(other);
    }

    /**
     * 网络序列化：只写 payload（typeId 由 IStackKey.serializeCommon 写）
     */
    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        boolean hasType = !this.gas.isEmptyType();
        buf.writeBoolean(hasType);
        if (!hasType) return;

        buf.writeRegistryId(MekanismAPI.gasRegistry(), this.gas);
    }

    @Override
    public @NotNull GasStackKey deserialize(FriendlyByteBuf buf)
    {
        boolean hasType = buf.readBoolean();
        if (!hasType) return EMPTY;

        Gas t = buf.readRegistryIdSafe(Gas.class);
        if (t == null || t.isEmptyType()) return EMPTY;

        return new GasStackKey(t);
    }

    /**
     * NBT 序列化：只写 payload（外层由 serializeNBTCommon 写 type）
     */
    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();
        ResourceLocation id = MekanismAPI.gasRegistry().getKey(this.gas);
        out.putString("gas", id == null ? "mekanism:empty_gas" : id.toString());
        return out;
    }

    @Override
    public @NotNull IStackKey<GasStack> deserializeNBT(CompoundTag nbt)
    {
        if (nbt == null) return EMPTY;

        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("gas"));
        Gas g = (id == null) ? MekanismAPI.EMPTY_GAS : MekanismAPI.gasRegistry().getValue(id);
        if (g == null) g = MekanismAPI.EMPTY_GAS;
        return new GasStackKey(g);
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return ChemicalStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull GasStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.gas.isEmptyType() ? GasStack.EMPTY : new GasStack(this.gas, 1);
        }

        if (this.gas.isEmptyType())
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = GasStack.EMPTY;
            }
            return GasStack.EMPTY;
        }

        GasStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getType() != this.gas)
        {
            this.clientCache = new GasStack(this.gas, 1);
            return this.clientCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof GasStackKey o) return this.gas == o.gas;
        return false;
    }

    @Override
    public int hashCode()
    {
        if (!hashReady)
        {
            // 与旧版一致：只看 type
            hashCache = 31 + gas.hashCode();
            hashReady = true;
        }
        return hashCache;
    }
}