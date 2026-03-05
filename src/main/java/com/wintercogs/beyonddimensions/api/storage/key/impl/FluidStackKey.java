package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.FluidStackKeyRender;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.DataComponentPatchHelper;
import com.wintercogs.beyonddimensions.util.RegistryAccessResolver;
import com.wintercogs.beyonddimensions.util.RegistryUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class FluidStackKey implements IStackKey<FluidStack>
{
    public static final Identifier ID = BeyondDimensions.makeId("stack_type/fluid");
    public static final FluidStackKey EMPTY = new FluidStackKey();

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    private static final MapCodec<FluidStackKey> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid")
                            .forGetter(t -> RegistryUtil.holderOf(t.fluid)),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(t -> t.patch)
            ).apply(instance, (holder, patch) -> new FluidStackKey(holder.value(), patch))
    );

    public static final Codec<FluidStackKey> CODEC = TYPE_CODEC.codec();

    // 实际不可变要素
    private final Fluid fluid;
    private final DataComponentPatch patch;

    // equals/hash 的规范化快照
    private volatile byte[] patchByte = new byte[0];
    private transient volatile WeakReference<HolderLookup.Provider> equalsByteProviderRef = null;

    // 渲染/便捷复制用的客户端缓存（amount 仅用于渲染，不参与 key 语义）
    private FluidStack serverCache; // （懒加载）
    private FluidStack clientCache; // （懒加载）
    private int hashCodeCache = 0;

    private FluidStackKey()
    {
        this(Fluids.EMPTY, DataComponentPatch.EMPTY);
    }

    public FluidStackKey(FluidStack stack)
    {
        this(stack.getFluid(), stack.getComponentsPatch());
    }

    private FluidStackKey(Fluid fluid, DataComponentPatch patch)
    {
        this.fluid = fluid;
        this.patch = (patch == null) ? DataComponentPatch.EMPTY : patch;
    }

    // ===== IStackKey =====

    @Override
    public Identifier getTypeId()
    {
        return ID;
    }

    @Override
    public MapCodec<FluidStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof FluidStack fluidStack)
            return new KeyAmount(new FluidStackKey(fluidStack), fluidStack.getAmount());
        return null;
    }

    @Override
    public @Nullable FluidStackKey fromSourceObject(Object key, DataComponentPatch dataComponentPatch)
    {
        if (key instanceof Fluid f)
        {
            DataComponentPatch p = (dataComponentPatch != null && !dataComponentPatch.isEmpty()) ? dataComponentPatch : DataComponentPatch.EMPTY;
            return new FluidStackKey(f, p);
        }
        return null;
    }

    @Override
    public FluidStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(RegistryUtil.holderOf(this.fluid), 1, this.patch);
        }

        if (this.fluid == Fluids.EMPTY)
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = FluidStack.EMPTY;
            }
            return FluidStack.EMPTY;
        }

        FluidStack cache = this.serverCache;
        if (cache.isEmpty() || cache.getFluid() != this.fluid)
        {
            this.serverCache = new FluidStack(RegistryUtil.holderOf(this.fluid), 1, this.patch);
            return this.serverCache;
        }

        // 非 EMPTY：返回前保证 amount >= 1（部分版本对 EMPTY.setAmount 会抛错）
        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<FluidStack> getStackClass()
    {
        return FluidStack.class;
    }

    @Override
    public @NotNull Fluid getSource()
    {
        return fluid;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Fluid.class;
    }

    @Override
    public String getModId()
    {
        return BuiltInRegistries.FLUID.getKey(fluid).getNamespace();
    }

    @Override
    public boolean isEmpty()
    {
        return this == FluidStackKey.EMPTY || this.fluid == Fluids.EMPTY;
    }

    @Override
    public FluidStackKey getEmpty()
    {
        return FluidStackKey.EMPTY;
    }

    @Override
    public FluidStack getEmptyStack()
    {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public FluidStack copyStackWithCount(long count)
    {
        if (this.fluid == Fluids.EMPTY) return FluidStack.EMPTY;
        return new FluidStack(RegistryUtil.holderOf(this.fluid), BDMath.clampLongToInt(count), this.patch);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        // 64桶一格
        return Math.min(64_000L, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        if (tagKey == null || this.fluid == Fluids.EMPTY) return false;
        if (!tagKey.isFor(Registries.FLUID)) return false;
        @SuppressWarnings("unchecked")
        TagKey<Fluid> fluidTag = (TagKey<Fluid>) tagKey;
        return RegistryUtil.holderOf(this.fluid).is(fluidTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return RegistryUtil.holderOf(this.fluid).tags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof FluidStackKey o)
        {
            return this.fluid == o.fluid;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof FluidStackKey o)
        {
            if (this.fluid != o.fluid) return false;

            // 规范化组件字节对比（Provider 稳定时避免重复计算）
            this.ensureByte();
            o.ensureByte();

            if (this.patchByte != null && this.patchByte.length > 0
                    && o.patchByte != null && o.patchByte.length > 0)
            {
                return Arrays.equals(this.patchByte, o.patchByte);
            }
            // 兜底：Provider 未就绪/异常，退回值语义
            return Objects.equals(this.patch, o.patch);
        }
        return false;
    }

    // ===== 网络序列化（新形态：类型ID + hasFluid + fluid RL + components）=====

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {

        boolean hasFluid = this.fluid != Fluids.EMPTY;
        buf.writeBoolean(hasFluid);
        if (!hasFluid) return;

        Identifier key = BuiltInRegistries.FLUID.getKey(this.fluid);
        buf.writeIdentifier(key);
        DataComponentPatch.STREAM_CODEC.encode(buf, patch);
    }

    @Override
    public @NotNull FluidStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        boolean hasFluid = buf.readBoolean();
        if (!hasFluid) return new FluidStackKey(Fluids.EMPTY, DataComponentPatch.EMPTY);

        Identifier key = buf.readIdentifier();
        Fluid f = BuiltInRegistries.FLUID.getOptional(key).orElse(Fluids.EMPTY);
        DataComponentPatch p = DataComponentPatch.STREAM_CODEC.decode(buf);
        return new FluidStackKey(f, p);
    }

    // ===== NBT 序列化：仅写新格式（fluid / components），无额外兜底 =====
    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        try
        {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.encodeStart(ops, this)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "FluidStackKey 序列化(Codec)出错: {}", err))
                    .map(nbt -> {
                        if (nbt instanceof CompoundTag ct) return ct; // 期望产物
                        BeyondDimensions.LOGGER.error(
                                "FluidStackKey 序列化得到的 NBT 非 CompoundTag，已丢弃该结果: {}",
                                nbt.getClass().getName());
                        return new CompoundTag();
                    })
                    .orElseGet(CompoundTag::new); // 编码失败 -> 空 Compound
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("FluidStackKey 序列化时出错: {}", t.getMessage(), t);
            return new CompoundTag();
        }
    }

    // ===== NBT 反序列化：直接交给 CODEC（TYPE_CODEC 内部已做新旧兼容）=====
    @Override
    public @NotNull FluidStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        try
        {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.parse(ops, nbt)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "FluidStackKey 反序列化(Codec)出错: {} | Keys={}", err, nbt.keySet()))
                    .orElse(FluidStackKey.EMPTY);
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("FluidStackKey 反序列化错误。Keys={} Error={}",
                    nbt.keySet(), t.getMessage(), t);
            return FluidStackKey.EMPTY;
        }
    }

    // ===== 渲染支持：交给外部渲染器；仅提供一个稳定的最小量副本 =====

    @Override
    public @NotNull IStackRender getRender()
    {
        // 与 ItemStackKey 一致，采用单独渲染器（请在你的渲染模块提供 FluidStackKeyRender.INSTANCE）
        return FluidStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull FluidStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(RegistryUtil.holderOf(this.fluid), 1, this.patch);
        }

        if (this.fluid == Fluids.EMPTY)
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = FluidStack.EMPTY;
            }
            return FluidStack.EMPTY;
        }

        FluidStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getFluid() != this.fluid)
        {
            this.clientCache = new FluidStack(RegistryUtil.holderOf(this.fluid), 1, this.patch);
            return this.clientCache;
        }

        // 非 EMPTY：返回前保证 amount >= 1（部分版本对 EMPTY.setAmount 会抛错）
        cache.setAmount(1);
        return cache;
    }

    // ===== equals/hashCode：以 fluid + 规范化 components 为准（不含数量）=====

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof FluidStackKey o)
        {
            return isSameTypeSameComponents(o);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        if (hashCodeCache == 0 || this.patchByte == null || this.patchByte.length == 0)
        {
            ensureByte();
            int base = 31 + fluid.hashCode();
            int patchPart = (this.patchByte != null && this.patchByte.length > 0)
                    ? Arrays.hashCode(this.patchByte)
                    : patch.hashCode();
            hashCodeCache = 31 * base + patchPart;
        }
        return hashCodeCache;
    }

    // ===== 规范化快照计算 =====

    private void ensureByte()
    {
        HolderLookup.Provider current = null;
        try
        {
            current = RegistryAccessResolver.resolve();
        }
        catch (Throwable ignored)
        {
        }

        HolderLookup.Provider cached = (equalsByteProviderRef != null) ? equalsByteProviderRef.get() : null;
        if (this.patchByte != null && this.patchByte.length > 0 && cached != null && cached == current)
        {
            return;
        }

        try
        {
            HolderLookup.Provider use = (current != null) ? current : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            byte[] out = DataComponentPatchHelper.toCanonicalBytes(this.patch, use);

            // 客户端兜底：若失败再尝试连接提供者
            if (out.length == 0 && FMLEnvironment.getDist() == Dist.CLIENT)
            {
                var mc = net.minecraft.client.Minecraft.getInstance();
                var conn = mc.getConnection();
                if (conn != null)
                {
                    HolderLookup.Provider connProv = conn.registryAccess();
                    if (connProv != use)
                    {
                        byte[] retry = DataComponentPatchHelper.toCanonicalBytes(this.patch, connProv);
                        if (retry.length > 0)
                        {
                            out = retry;
                            use = connProv;
                        }
                    }
                }
            }

            this.patchByte = out;
            this.equalsByteProviderRef = (out.length > 0) ? new WeakReference<>(use) : null;
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.warn("FluidStackKey 组件规范化失败: {}", t.toString());
            this.patchByte = new byte[0];
            this.equalsByteProviderRef = null;
        }
    }
}
