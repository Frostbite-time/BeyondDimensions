package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ChemicalStackKey implements IStackKey<ChemicalStack> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/chemical");
    public static final ChemicalStackKey EMPTY = new ChemicalStackKey();

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    // 新格式：仅化学品注册名
    private static final MapCodec<ChemicalStackKey> NEW_FMT = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("chemical")
                            .forGetter(t -> MekanismAPI.CHEMICAL_REGISTRY.getKey(t.chemical))
            ).apply(instance, (rl) -> {
                Chemical c = MekanismAPI.CHEMICAL_REGISTRY.get(rl);
                return new ChemicalStackKey(c);
            })
    );

    /**
     * 最终产出的 MapCodec（编码只写新格式；解码按代次兼容）
     * 兼容层级：
     *  - 【新格式 v3】"chemical"
     *  - 【兼容 G0 v1】旧形态：直接内嵌 ChemicalStack
     *      · JSON/Codec： "internal_stack"
     *      · 旧 NBT 写法： "Stack"（并伴随 "Amount"；Key 层忽略）
     */
    public static final MapCodec<ChemicalStackKey> TYPE_CODEC = new MapCodec<>() {

        // 统一键名
        private static final String K_CHEM     = "chemical";

        // 【兼容 G0】早期把整个 ChemicalStack 内嵌
        private static final String K_LEGACY   = "internal_stack"; // 旧 JSON/Codec
        private static final String K_STACK    = "Stack";          // 旧 NBT 写法
        // 旧数量键（Key 层不需要，读取时忽略）
        private static final String K_AMOUNT   = "Amount";
        private static final String K_amount   = "amount";

        @Override
        public <T> DataResult<ChemicalStackKey> decode(DynamicOps<T> ops, MapLike<T> input) {
            final T kChem   = ops.createString(K_CHEM);
            final T kLegacy = ops.createString(K_LEGACY);
            final T kStack  = ops.createString(K_STACK);
            final T kAmt    = ops.createString(K_AMOUNT);
            final T kamt    = ops.createString(K_amount);

            // ─────────────────────────────────────────
            // 【新格式 v3】chemical
            // ─────────────────────────────────────────
            if (input.get(kChem) != null) {
                return NEW_FMT.decode(ops, input);
            }

            // ─────────────────────────────────────────
            // 【兼容 G0 v1】直接内嵌 ChemicalStack（忽略 Amount/amount）
            // 形态 A：internal_stack（旧 JSON/Codec）
            // 形态 B：Stack          （旧 NBT）
            // 处理：解析出 ChemicalStack，然后 new ChemicalStackKey(ChemicalStack)
            // 删除时机：确认旧档已全部迁移/重存为新格式后可删除
            // ─────────────────────────────────────────
            T legacyNode = input.get(kLegacy);
            if (legacyNode == null) legacyNode = input.get(kStack);
            if (legacyNode != null) {
                // 注意：旧数据里可能同时带有 Amount/amount —— 在 Key 层直接忽略
                return ChemicalStack.OPTIONAL_CODEC.parse(ops, legacyNode)
                        .map(ChemicalStackKey::new);
            }

            // 默认：让 NEW_FMT 给出“缺少 chemical”的明确错误
            return NEW_FMT.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(ChemicalStackKey value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            // 仅写新格式（chemical）
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops) {
            // 对外暴露新格式键集合
            return java.util.stream.Stream.of(K_CHEM).map(ops::createString);
        }
    };

    public static final Codec<ChemicalStackKey> CODEC = TYPE_CODEC.codec();

    // —— 实际不可变要素 —— //
    private final Chemical chemical;

    // 客户端渲染/复制缓存（amount≥1，仅用于显示，不参与 Key 语义）
    private ChemicalStack severCache;
    private ChemicalStack clientCache;
    private int hashCodeCache = 0;

    private ChemicalStackKey()
    {
        this(MekanismAPI.EMPTY_CHEMICAL);
    }

    public ChemicalStackKey(ChemicalStack stack)
    {
        this(stack.getChemical());
    }

    private ChemicalStackKey(Chemical chemical)
    {
        this.chemical = (chemical == null) ? MekanismAPI.EMPTY_CHEMICAL : chemical;
    }

    // ===== IStackKey =====

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public MapCodec<ChemicalStackKey> codec() {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if(stack instanceof ChemicalStack chemicalStack)
            return new KeyAmount(new ChemicalStackKey(chemicalStack), chemicalStack.getAmount());
        return null;
    }

    @Override
    public @Nullable ChemicalStackKey fromSourceObject(Object key, DataComponentPatch ignored)
    {
        if (key instanceof Chemical c) {
            return new ChemicalStackKey(c);
        }
        return null;
    }

    @Override
    public ChemicalStack getReadOnlyStack()
    {
        if(severCache == null)
        {
            this.severCache = this.chemical.isEmptyType() ? ChemicalStack.EMPTY : new ChemicalStack(this.chemical, 1);
        }

        if (chemical.isEmptyType()) {
            if (!severCache.isEmpty()) severCache = ChemicalStack.EMPTY;
            return ChemicalStack.EMPTY;
        }
        ChemicalStack cache = severCache;
        if (cache.isEmpty() || cache.getChemical() != this.chemical) {
            severCache = new ChemicalStack(this.chemical, 1);
            return severCache;
        }
        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<ChemicalStack> getStackClass() {
        return ChemicalStack.class;
    }

    @Override
    public @NotNull Chemical getSource() {
        return chemical;
    }

    @Override
    public Class<?> getSourceClass() {
        return Chemical.class;
    }

    @Override
    public String getModId()
    {
        return MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical).getNamespace();
    }

    @Override
    public boolean isEmpty() {
        return this == ChemicalStackKey.EMPTY || chemical.isEmptyType();
    }

    @Override
    public ChemicalStackKey getEmpty() {
        return ChemicalStackKey.EMPTY;
    }

    @Override
    public ChemicalStack getEmptyStack() {
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack copyStack() {
        return copyStackWithCount(1);
    }

    @Override
    public ChemicalStack copyStackWithCount(long count) {
        if (chemical.isEmptyType()) return ChemicalStack.EMPTY;
        long amt = Math.max(1, Math.min(Integer.MAX_VALUE, count));
        return new ChemicalStack(chemical, amt);
    }

    @Override
    public long getVanillaMaxStackSize() {
        // 仅用于 UI/逻辑辅助（不参与 Key）
        return Math.min(64_000L, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        if (tagKey == null || chemical.isEmptyType()) return false;
        if (!tagKey.isFor(MekanismAPI.CHEMICAL_REGISTRY_NAME)) return false;
        @SuppressWarnings("unchecked")
        TagKey<Chemical> chemicalTag = (TagKey<Chemical>) tagKey;
        return chemical.getAsHolder().is(chemicalTag);
    }

    @Override
    public boolean isSame(IStackKey<?> other) {
        if (this == other) return true;
        if (other instanceof ChemicalStackKey o) {
            return this.chemical == o.chemical;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other) {
        // Chemical 无额外组件，组件相等与 isSame 一致
        return isSame(other);
    }

    // —— 网络序列化（新形态）——

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        boolean has = !chemical.isEmptyType();
        buf.writeBoolean(has);
        if (!has) return;

        ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        buf.writeResourceLocation(rl);
    }

    @Override
    public @NotNull ChemicalStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        boolean has = buf.readBoolean();
        if (!has) return ChemicalStackKey.EMPTY;

        ResourceLocation rl = buf.readResourceLocation();
        Chemical c = MekanismAPI.CHEMICAL_REGISTRY.get(rl);
        return new ChemicalStackKey(c);
    }

    // —— NBT：写新；读新优先 + 旧兼容 —— //
    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        try {
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
        } catch (Throwable t) {
            BeyondDimensions.LOGGER.error("ChemicalStackKey 序列化时出错: {}", t.getMessage(), t);
            return new CompoundTag();
        }
    }

    @Override
    public @NotNull ChemicalStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        try {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.parse(ops, nbt)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "ChemicalStackKey 反序列化(Codec)出错: {} | Keys={}", err, nbt.getAllKeys()))
                    .orElse(ChemicalStackKey.EMPTY);
        } catch (Throwable t) {
            BeyondDimensions.LOGGER.error("ChemicalStackKey 反序列化错误。Keys={} Error={}",
                    nbt.getAllKeys(), t.getMessage(), t);
            return ChemicalStackKey.EMPTY;
        }
    }

    // —— 渲染支持 —— //
    @Override
    public @NotNull IStackRender getRender() {
        return ChemicalStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull ChemicalStack getRenderStack() {
        if(clientCache == null)
        {
            this.clientCache = this.chemical.isEmptyType() ? ChemicalStack.EMPTY : new ChemicalStack(this.chemical, 1);
        }

        if (chemical.isEmptyType()) {
            if (!clientCache.isEmpty()) clientCache = ChemicalStack.EMPTY;
            return ChemicalStack.EMPTY;
        }
        ChemicalStack cache = clientCache;
        if (cache.isEmpty() || cache.getChemical() != this.chemical) {
            clientCache = new ChemicalStack(this.chemical, 1);
            return clientCache;
        }
        cache.setAmount(1);
        return cache;
    }

    // —— equals/hash —— //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ChemicalStackKey k) {
            return this.chemical == k.chemical;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        if(hashCodeCache == 0)
        {
            hashCodeCache = 31 + Objects.hashCode(this.chemical);
        }
        return hashCodeCache;
    }
}