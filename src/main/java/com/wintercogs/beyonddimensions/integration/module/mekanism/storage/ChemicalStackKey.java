package com.wintercogs.beyonddimensions.integration.module.mekanism.storage;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class ChemicalStackKey implements IStackKey<ChemicalStack>
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "stack_type/chemical");
    public static final ChemicalStackKey EMPTY = new ChemicalStackKey();

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    private static final MapCodec<ChemicalStackKey> NEW_FMT = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("chemical")
                            .forGetter(t -> t.chemical.unwrapKey()
                                    .map(ResourceKey::location)
                                    .orElseThrow(() -> new IllegalStateException("Unbound chemical holder")))
            ).apply(instance, (rl) -> {
                ResourceKey<Chemical> key = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl);
                Holder<Chemical> holder = MekanismAPI.CHEMICAL_REGISTRY.getHolder(key)
                        .<Holder<Chemical>>map(r -> r)
                        .orElse(MekanismAPI.EMPTY_CHEMICAL_HOLDER);
                return new ChemicalStackKey(holder);
            })
    );

    /**
     * 最终产出的 MapCodec（编码只写新格式；解码按代次兼容）
     */
    public static final MapCodec<ChemicalStackKey> TYPE_CODEC = new MapCodec<>()
    {
        // 统一键名
        private static final String K_CHEM = "chemical";

        // 早期键名
        private static final String K_LEGACY = "internal_stack";
        private static final String K_STACK = "Stack";

        @Override
        public <T> DataResult<ChemicalStackKey> decode(DynamicOps<T> ops, MapLike<T> input)
        {
            final T kChem = ops.createString(K_CHEM);
            final T kLegacy = ops.createString(K_LEGACY);
            final T kStack = ops.createString(K_STACK);

            // 新格式
            if (input.get(kChem) != null)
            {
                return NEW_FMT.decode(ops, input);
            }

            // 旧格式
            T legacyNode = input.get(kLegacy);
            if (legacyNode == null) legacyNode = input.get(kStack);
            if (legacyNode != null)
            {
                return ChemicalStack.OPTIONAL_CODEC.parse(ops, legacyNode)
                        .map(ChemicalStackKey::new);
            }

            // 回退
            return NEW_FMT.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(ChemicalStackKey value, DynamicOps<T> ops, RecordBuilder<T> prefix)
        {
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops)
        {
            return java.util.stream.Stream.of(K_CHEM).map(ops::createString);
        }
    };

    public static final Codec<ChemicalStackKey> CODEC = TYPE_CODEC.codec();

    // 唯一实际存储
    private final Holder<Chemical> chemical;

    // 对外缓存
    private ChemicalStack serverCache;
    private ChemicalStack clientCache;
    private int hashCodeCache = 0;

    private ChemicalStackKey()
    {
        this(MekanismAPI.EMPTY_CHEMICAL_HOLDER);
    }

    public ChemicalStackKey(ChemicalStack stack)
    {
        this(stack.getChemicalHolder());
    }

    private ChemicalStackKey(Holder<Chemical> chemical)
    {
        this.chemical = resolveBound((chemical == null) ? MekanismAPI.EMPTY_CHEMICAL_HOLDER : chemical);
    }

    // ===== IStackKey =====

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public MapCodec<ChemicalStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof ChemicalStack chemicalStack)
            return new KeyAmount(new ChemicalStackKey(chemicalStack), chemicalStack.getAmount());
        return null;
    }

    @Override
    public @Nullable ChemicalStackKey fromSourceObject(Object key, DataComponentPatch ignored)
    {
        if (key instanceof Holder<?> cHolder)
        {
            boolean isChemHolder = cHolder.unwrapKey()
                    .map(ResourceKey::registryKey)
                    .filter(MekanismAPI.CHEMICAL_REGISTRY_NAME::equals)
                    .isPresent();
            if (isChemHolder)
            {
                @SuppressWarnings("unchecked")
                Holder<Chemical> ch = (Holder<Chemical>) cHolder;
                return new ChemicalStackKey(ch);
            }
        }
        else if (key instanceof Chemical c)
        {
            ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(c);
            ResourceKey<Chemical> rkey = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl);
            return MekanismAPI.CHEMICAL_REGISTRY.getHolder(rkey)
                    .map(ChemicalStackKey::new)
                    .orElse(null);
        }
        return null;
    }

    @Override
    public ChemicalStack getReadOnlyStack()
    {
        if (serverCache == null)
        {
            this.serverCache = this.chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER ? ChemicalStack.EMPTY : new ChemicalStack(this.chemical, 1);
        }

        if (chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER)
        {
            if (!serverCache.isEmpty()) serverCache = ChemicalStack.EMPTY;
            return ChemicalStack.EMPTY;
        }
        ChemicalStack cache = serverCache;
        if (cache.isEmpty() || cache.getChemical() != this.chemical.value())
        {
            serverCache = new ChemicalStack(this.chemical, 1);
            return serverCache;
        }
        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<ChemicalStack> getStackClass()
    {
        return ChemicalStack.class;
    }

    @Override
    public @NotNull Chemical getSource()
    {
        return chemical.value();
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Chemical.class;
    }

    @Override
    public String getModId()
    {
        return chemical.unwrapKey()
                .map(k -> k.location().getNamespace())
                .orElse(OtherModIds.MEKANISM);
    }

    @Override
    public boolean isEmpty()
    {
        return this == ChemicalStackKey.EMPTY || chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER;
    }

    @Override
    public ChemicalStackKey getEmpty()
    {
        return ChemicalStackKey.EMPTY;
    }

    @Override
    public ChemicalStack getEmptyStack()
    {
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public ChemicalStack copyStackWithCount(long count)
    {
        if (chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER) return ChemicalStack.EMPTY;
        return new ChemicalStack(chemical, count);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
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
        if (tagKey == null || chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER) return false;
        if (!tagKey.isFor(MekanismAPI.CHEMICAL_REGISTRY_NAME)) return false;
        @SuppressWarnings("unchecked")
        TagKey<Chemical> chemicalTag = (TagKey<Chemical>) tagKey;
        return chemical.is(chemicalTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return chemical.tags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof ChemicalStackKey o)
        {
            return this.chemical == o.chemical;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        // Chemical 无额外组件，组件相等与 isSame 一致
        return isSame(other);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        boolean has = chemical != MekanismAPI.EMPTY_CHEMICAL_HOLDER;
        buf.writeBoolean(has);
        if (!has) return;

        Chemical.HOLDER_STREAM_CODEC.encode(buf, chemical);
    }

    @Override
    public @NotNull ChemicalStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        boolean has = buf.readBoolean();
        if (!has) return ChemicalStackKey.EMPTY;

        Holder<Chemical> chemicalHolder = Chemical.HOLDER_STREAM_CODEC.decode(buf);
        return new ChemicalStackKey(chemicalHolder);
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        try
        {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.encodeStart(ops, this)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "ChemicalStackKey 序列化(Codec)出错: {}", err))
                    .map(nbt -> {
                        if (nbt instanceof CompoundTag ct) return ct; // 期望产物
                        BeyondDimensions.LOGGER.error(
                                "ChemicalStackKey 序列化得到的 NBT 非 CompoundTag，已丢弃该结果: {}",
                                nbt.getClass().getName());
                        return new CompoundTag();
                    })
                    .orElseGet(CompoundTag::new); // 编码失败 -> 空 Compound
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("ChemicalStackKey 序列化时出错: {}", t.getMessage(), t);
            return new CompoundTag();
        }
    }

    @Override
    public @NotNull ChemicalStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        try
        {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.parse(ops, nbt)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "ChemicalStackKey 反序列化(Codec)出错: {} | Keys={}", err, nbt.getAllKeys()))
                    .orElse(ChemicalStackKey.EMPTY);
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("ChemicalStackKey 反序列化错误。Keys={} Error={}",
                    nbt.getAllKeys(), t.getMessage(), t);
            return ChemicalStackKey.EMPTY;
        }
    }

    // —— 渲染支持 —— //
    @Override
    public @NotNull IStackRender getRender()
    {
        return ChemicalStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull ChemicalStack getRenderStack()
    {
        if (clientCache == null)
        {
            this.clientCache = this.chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER ? ChemicalStack.EMPTY : new ChemicalStack(this.chemical, 1);
        }

        if (chemical == MekanismAPI.EMPTY_CHEMICAL_HOLDER)
        {
            if (!clientCache.isEmpty()) clientCache = ChemicalStack.EMPTY;
            return ChemicalStack.EMPTY;
        }
        ChemicalStack cache = clientCache;
        if (cache.isEmpty() || cache.getChemical() != this.chemical.value())
        {
            clientCache = new ChemicalStack(this.chemical, 1);
            return clientCache;
        }
        cache.setAmount(1);
        return cache;
    }

    // —— equals/hash —— //

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ChemicalStackKey otherKey)) return false;
        var a = this.chemical.unwrapKey();
        var b = otherKey.chemical.unwrapKey();
        if (a.isPresent() && b.isPresent()) return a.get().equals(b.get());
        return this.chemical.value() == otherKey.chemical.value();
    }

    @Override
    public int hashCode()
    {
        if (hashCodeCache == 0)
        {
            hashCodeCache = this.chemical.unwrapKey()
                    .map(Object::hashCode)
                    .orElse(System.identityHashCode(this.chemical.value()));
        }
        return hashCodeCache;
    }

    /**
     * 确保当前holder已经进入注册表
     */
    private static Holder<Chemical> resolveBound(Holder<Chemical> h)
    {
        if (h.unwrapKey().isPresent()) return h; // 已绑定
        ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(h.value());
        ResourceKey<Chemical> rk = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl);
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(rk)
                .map(r -> (Holder<Chemical>) r)
                .orElse(MekanismAPI.EMPTY_CHEMICAL_HOLDER);
    }
}